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

import nxt.Nxt;
import nxt.NxtException;
import nxt.configuration.ConfigParser;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.ResourceLookup;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

public class SetConfiguration extends APIServlet.APIRequestHandler {

    static final SetConfiguration instance = new SetConfiguration();

    private SetConfiguration() {
        super(new APITag[]{APITag.UTILS}, "propertiesJSON", "shutdown");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        JSONArray properties = ParameterParser.getJsonArray(request, "propertiesJSON");
        if (properties == null) {
            return JSONResponses.missing("propertiesJSON");
        }
        boolean isShutdown = Boolean.parseBoolean(request.getParameter("shutdown"));

        Properties temp = new Properties();
        String propertiesPath = Nxt.loadProperties(temp, Nxt.NXT_PROPERTIES, false);

        for (Object o : properties) {
            JSONObject propertyJSON = (JSONObject) o;
            String property = (String) propertyJSON.get("property");
            String value = Convert.emptyToNull((String) propertyJSON.get("value"));
            if (value != null && !ConfigParser.getInstance().isValidPropertyValue(property, value)) {
                return JSONResponses.incorrect("value", "for property " + property);
            }

            if (value == null) {
                temp.remove(property);
            } else {
                temp.setProperty(property, value);
            }
        }

        if (propertiesPath == null) {
            propertiesPath = getPropertiesPathFromClasspath();
        }
        try (FileOutputStream out = new FileOutputStream(propertiesPath)) {
            temp.store(out, "Any changes to this file require a node restart.\n" +
                    "If you are using the Node Configuration editor this file will be overwritten and any custom comments and ordering lost");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (isShutdown) {
            new Thread(() -> System.exit(0)).start();
        }

        JSONObject response = new JSONObject();
        response.put("shutdown", isShutdown);
        return response;
    }

    private String getPropertiesPathFromClasspath() {
        try {
            URL url = ResourceLookup.getSystemResource(Nxt.NXT_PROPERTIES);
            if (url != null) {
                File fileFromClasspath = new File(url.toURI());
                if (fileFromClasspath.canWrite()) {
                    return fileFromClasspath.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Logger.logWarningMessage("Seems I can't write to the property file on the classpath.", e);
        }
        Logger.logInfoMessage("Can't write into the classpath, reverting to the default path");
        return Paths.get(Nxt.CONFIG_DIR, Nxt.NXT_PROPERTIES).toString();
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected boolean isChainSpecific() {
        return false;
    }
}
