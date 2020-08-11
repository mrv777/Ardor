/*
 * Copyright © 2020 Jelurida IP B.V.
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
package nxt.http.proxy;

import nxt.Nxt;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;

public class ResponseConfirmation {
    public enum Event {
        CONFIRMATION, REJECTION
    }

    private static final Listeners<ResponseConfirmation, Event> listeners = new Listeners<>();

    private final String requestType;
    private final String requestQuery;
    private final byte[] requestData;
    private final ComparableResponse originalResponse;
    private final HttpClient httpClient;
    private final String method;
    private final String contentType;
    private final ConfirmationReport report;
    private final URI initialNodeUri;

    public static boolean addListener(Listener<ResponseConfirmation> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<ResponseConfirmation> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public ResponseConfirmation(HttpClient httpClient, HttpServletRequest clientRequest) {
        this.requestType = (String) clientRequest.getAttribute(Attr.REQUEST_TYPE);
        this.requestQuery = clientRequest.getQueryString();
        this.requestData = (byte[]) clientRequest.getAttribute(Attr.POST_DATA);
        this.originalResponse = (ComparableResponse) clientRequest.getAttribute(Attr.RESPONSE_CONTENT);
        this.method = clientRequest.getMethod();
        this.contentType = clientRequest.getContentType();
        this.httpClient = httpClient;
        this.report = new ConfirmationReport(this.requestType, Nxt.getEpochTime());
        this.initialNodeUri = (URI) clientRequest.getAttribute(Attr.REMOTE_URL);
    }

    public void startConfirmation(List<URI> nodesUrls, int idleTimeout) {
        httpClient.setIdleTimeout(idleTimeout);
        nodesUrls.forEach(nodeUrl -> httpClient.newRequest(nodeUrl.toString() + "/nxt?" + requestQuery)
                .method(method)
                .content(new BytesContentProvider(contentType, requestData))
                .send(new ResponseListener(nodeUrl)));
    }
    
    private class ResponseListener extends BufferingResponseListener {

        private final URI nodeUrl;

        public ResponseListener(URI nodeUrl) {
            super(BufferingContentTransformer.MAX_CONFIRMABLE_CONTENT_LENGTH);
            this.nodeUrl = nodeUrl;
        }

        @Override
        public void onComplete(Result result) {
            if (result.isSucceeded()) {
                try {
                    ComparableResponse response = new ComparableResponse(requestType, getContent());
                    if (response.isConfirming(originalResponse)) {
                        report.addConfirmingNode(nodeUrl);
                        listeners.notify(ResponseConfirmation.this, Event.CONFIRMATION);
                    } else {
                        Logger.logDebugMessage("Rejection for url %s request %s data %s differ from response of %s", nodeUrl, requestType, new String(requestData), initialNodeUri.toString());
                        Logger.logDebugMessage("Orig response %s", originalResponse.toString());
                        Logger.logDebugMessage("This response %s", response.toString());
                        report.addRejectingNode(nodeUrl);
                        listeners.notify(ResponseConfirmation.this, Event.REJECTION);
                    }
                } catch (Exception e) {
                    Logger.logDebugMessage("Exception while confirming " + requestType + " by " + nodeUrl, e);
                }
            } else {
                Logger.logDebugMessage("Remote node %s connection failed for request %s: %s", nodeUrl, requestType, result.getFailure());
            }
        }
    }
    
    /**
     * @return The address of the node which provided the initial response, the one which is being confirmed
     */
    public String getInitialNode() {
        return initialNodeUri.getHost();
    }

    public ConfirmationReport getReport() {
        return report;
    }
}
