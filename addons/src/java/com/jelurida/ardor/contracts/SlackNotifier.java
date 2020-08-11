/*
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

package com.jelurida.ardor.contracts;

import nxt.addons.AbstractContract;
import nxt.addons.ContractRunnerParameter;
import nxt.addons.ContractSetupParameter;
import nxt.addons.DelegatedContext;
import nxt.addons.JO;

import java.net.HttpURLConnection;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Post a message to slack using a web hook.
 */
public class SlackNotifier extends AbstractContract<String, Integer> {

    public interface Parameters {
        @ContractRunnerParameter
        @ContractSetupParameter
        String slackWebHookUrl();
    }

    /**
     * Process internal invocation by another contract
     * @param context the context delegated by the invoking contract
     * @param text the text to post on slack
     * @return response code
     */
    @Override
    public Integer processInvocation(DelegatedContext context, String text) {
        Parameters params = context.getParams(Parameters.class);
        String slackWebHookUrl = params.slackWebHookUrl();
        if (slackWebHookUrl == null) {
            context.logInfoMessage("Slack Webhook not configured, message is '%s'", text);
            return -1;
        }
        try {
            JO payload = buildSlackPayload(text);
            byte[] bytes = payload.toJSONString().getBytes(UTF_8);
            URL url = new URL(slackWebHookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            connection.setDoOutput(true);
            connection.getOutputStream().write(bytes);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                context.logErrorMessage(new Exception(String.format("Response from Slack API: %s:%s", responseCode, connection.getResponseMessage())));
            }
            return responseCode;
        } catch (Throwable t) {
            context.logErrorMessage(t);
            return 500;
        }
    }

    private static JO buildSlackPayload(String codeBlock) {
        JO json = new JO();
        json.put("text", codeBlock);
        return json;
    }
}
