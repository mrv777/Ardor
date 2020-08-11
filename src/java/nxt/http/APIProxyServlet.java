/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.http;

import nxt.http.proxy.Attr;
import nxt.http.proxy.PasswordDetectedException;
import nxt.http.proxy.RequestContentTransformer;
import nxt.http.proxy.ResponseConfirmation;
import nxt.http.proxy.ResponseContentTransformer;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import nxt.util.security.BlockchainPermission;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.AsyncMiddleManServlet;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;

import static nxt.http.JSONResponses.ERROR_NOT_ALLOWED;

public final class APIProxyServlet extends AsyncMiddleManServlet {
    public static final String[] PREPROCESSED_SENSITIVE_PARAMS;
    static {
        PREPROCESSED_SENSITIVE_PARAMS = APIServlet.getSensitiveParams().stream().map(s -> s + "=").toArray(String[]::new);
    }
    static void initClass() {}

    private HttpClient confirmationsClient;

    @Override
    public void init(ServletConfig config) throws ServletException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new BlockchainPermission("api"));
        }
        super.init(config);
        config.getServletContext().setAttribute("apiServlet", new APIServlet());
        confirmationsClient = HttpClientFactory.newHttpClient();
        try {
            confirmationsClient.start();
        } catch (Exception e) {
            throw new ServletException("Failed to start confirmations HttpClient", e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            confirmationsClient.stop();
        } catch (Exception e) {
            Logger.logErrorMessage("Failed to stop confirmations client", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONStreamAware responseJson = null;
        try {
            if (API.isForbiddenHost(request.getRemoteHost())) {
                responseJson = ERROR_NOT_ALLOWED;
                return;
            }
            MultiMap<String> parameters = getRequestParameters(request);
            String requestType = getRequestType(parameters);
            if (APIProxy.isActivated() && isForwardable(requestType)) {
                if (APIServlet.isAnySensitiveParam(parameters.keySet())) {
                    throw new ParameterException(JSONResponses.PROXY_SECRET_DATA_DETECTED);
                }
                if (!initRemoteRequest(request, requestType)) {
                    if (Peers.getPeers(peer -> peer.getState() == Peer.State.CONNECTED, 1).size() >= 1) {
                        responseJson = JSONResponses.API_PROXY_NO_OPEN_API_PEERS;
                    } else {
                        responseJson = JSONResponses.API_PROXY_NO_PUBLIC_PEERS;
                    }
                } else {
                    super.service(request, response);
                }
            } else {
                APIServlet apiServlet = (APIServlet)request.getServletContext().getAttribute("apiServlet");
                apiServlet.service(request, response);
            }
        } catch (ParameterException e) {
            responseJson = e.getErrorResponse();
        } finally {
            if (responseJson != null) {
                try {
                    try (Writer writer = response.getWriter()) {
                        JSON.writeJSONString(responseJson, writer);
                    }
                } catch(IOException e) {
                    Logger.logInfoMessage("Failed to write response to client", e);
                }
            }
        }
    }

    private MultiMap<String> getRequestParameters(HttpServletRequest request) {
        MultiMap<String> parameters = new MultiMap<>();
        String queryString = request.getQueryString();
        if (queryString != null) {
            UrlEncoded.decodeUtf8To(queryString, parameters);
        }
        return parameters;
    }

    @Override
    protected void addProxyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {

    }

    @Override
    protected HttpClient newHttpClient() {
        return HttpClientFactory.newHttpClient();
    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        Integer timeout = (Integer) clientRequest.getAttribute(Attr.REMOTE_SERVER_IDLE_TIMEOUT);
        HttpClient httpClient = getHttpClient();
        if (timeout != null && httpClient != null) {
            httpClient.setIdleTimeout(Math.max(timeout - APIProxy.PROXY_IDLE_TIMEOUT_DELTA, 0));
        }

        URI remoteUrl = (URI) clientRequest.getAttribute(Attr.REMOTE_URL);
        return remoteUrl.toString();
    }

    @Override
    protected void onClientRequestFailure(HttpServletRequest clientRequest, Request proxyRequest,
                                          HttpServletResponse proxyResponse, Throwable failure) {
        if (failure instanceof PasswordDetectedException) {
            PasswordDetectedException passwordDetectedException = (PasswordDetectedException) failure;
            try (Writer writer = proxyResponse.getWriter()) {
                JSON.writeJSONString(passwordDetectedException.getErrorResponse(), writer);
                sendProxyResponseError(clientRequest, proxyResponse, HttpStatus.OK_200);
            } catch (IOException e) {
                e.addSuppressed(failure);
                super.onClientRequestFailure(clientRequest, proxyRequest, proxyResponse, e);
            }
        } else {
            super.onClientRequestFailure(clientRequest, proxyRequest, proxyResponse, failure);
        }
    }

    private String getRequestType(MultiMap<String> parameters) throws ParameterException {
        String requestType = parameters.getString("requestType");
        if (Convert.emptyToNull(requestType) == null) {
            throw new ParameterException(JSONResponses.PROXY_MISSING_REQUEST_TYPE);
        }

        APIServlet.APIRequestHandler apiRequestHandler = APIServlet.apiRequestHandlers.get(requestType);
        if (apiRequestHandler == null) {
            if (APIServlet.disabledRequestHandlers.containsKey(requestType)) {
                throw new ParameterException(JSONResponses.ERROR_DISABLED);
            } else {
                throw new ParameterException(JSONResponses.ERROR_INCORRECT_REQUEST);
            }
        }
        return requestType;
    }

    private boolean initRemoteRequest(HttpServletRequest clientRequest, String requestType) {
        StringBuilder uri;
        if (!APIProxy.forcedServerURL.isEmpty()) {
            uri = new StringBuilder();
            uri.append(APIProxy.forcedServerURL);
        } else {
            Peer servingPeer = APIProxy.getInstance().getServingPeer(requestType);
            if (servingPeer == null) {
                return false;
            }
            uri = servingPeer.getPeerApiUri();
            clientRequest.setAttribute(Attr.REMOTE_SERVER_IDLE_TIMEOUT, servingPeer.getApiServerIdleTimeout());
        }

        APIServlet.APIRequestHandler apiRequestHandler = APIServlet.apiRequestHandlers.get(requestType);
        clientRequest.setAttribute(Attr.REQUEST_NEEDS_CONFIRMATION, !apiRequestHandler.requirePost()
                && !APIProxy.NOT_CONFIRMED_REQUESTS.contains(requestType));
        clientRequest.setAttribute(Attr.REQUEST_TYPE, requestType);

        uri.append("/nxt");
        String query = clientRequest.getQueryString();
        if (query != null) {
            uri.append("?").append(query);
        }
        clientRequest.setAttribute(Attr.REMOTE_URL, URI.create(uri.toString()).normalize());
        return true;
    }

    private boolean isForwardable(String requestType) {
        APIServlet.APIRequestHandler apiRequestHandler = APIServlet.apiRequestHandlers.get(requestType);
        if (!apiRequestHandler.requireBlockchain()) {
            return false;
        }
        if (apiRequestHandler.requireFullClient()) {
            return false;
        }
        if (APIProxy.NOT_FORWARDED_REQUESTS.contains(requestType)) {
            return false;
        }

        return true;
    }

    @Override
    protected Response.Listener newProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
        return new APIProxyResponseListener(request, response);
    }

    private class APIProxyResponseListener extends AsyncMiddleManServlet.ProxyResponseListener {

        APIProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        @Override
        public void onFailure(Response response, Throwable failure) {
            super.onFailure(response, failure);
            Logger.logErrorMessage("proxy failed", failure);
            APIProxy.getInstance().blacklistHost(response.getRequest().getHost());
        }
    }

    @Override
    protected void onProxyResponseSuccess(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onProxyResponseSuccess(clientRequest, proxyResponse, serverResponse);
        if (Boolean.TRUE.equals(clientRequest.getAttribute(Attr.REQUEST_NEEDS_CONFIRMATION))) {
            APIProxy.getInstance().startResponseConfirmation(new ResponseConfirmation(confirmationsClient, clientRequest));
        }
    }

    @Override
    protected AsyncMiddleManServlet.ContentTransformer newClientRequestContentTransformer(
            HttpServletRequest clientRequest, Request proxyRequest) {
        if (isContentTransformRequired(clientRequest)) {
            return new RequestContentTransformer(clientRequest);
        } else {
            return super.newClientRequestContentTransformer(clientRequest, proxyRequest);
        }
    }

    @Override
    protected AsyncMiddleManServlet.ContentTransformer newServerResponseContentTransformer(
            HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        if (isContentTransformRequired(clientRequest)) {
            List<String> contentEncodings = serverResponse.getHeaders().getCSV(HttpHeader.CONTENT_ENCODING.asString(), false);
            ContentTransformer result = new ResponseContentTransformer(clientRequest);
            if (contentEncodings != null && contentEncodings.stream().anyMatch("gzip"::equalsIgnoreCase)) {
                result = new GZIPContentTransformer(getHttpClient(), result);
            }
            return result;
        } else {
            return super.newServerResponseContentTransformer(clientRequest, proxyResponse, serverResponse);
        }
    }

    private boolean isContentTransformRequired(HttpServletRequest clientRequest) {
        String contentType = clientRequest.getContentType();
        return (contentType == null || !contentType.contains("multipart"))
                && APIProxy.isActivated() && isForwardable((String) clientRequest.getAttribute(Attr.REQUEST_TYPE));
    }
}