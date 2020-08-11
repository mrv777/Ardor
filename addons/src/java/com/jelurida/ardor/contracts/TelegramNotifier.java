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
import nxt.addons.DelegatedContext;
import nxt.addons.InitializationContext;
import nxt.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Post a message to slack using a web hook.
 */
public class TelegramNotifier extends AbstractContract<String, Integer> {

    private String token;
    private String chatId;

    public interface Parameters {
        @ContractRunnerParameter
        String token();

        @ContractRunnerParameter
        String chatId();
    }

    @Override
    public void init(InitializationContext context) {
        Parameters params = context.getParams(Parameters.class);
        token = params.token();
        chatId = params.chatId();
    }

    /**
     * Process internal invocation by another contract
     * @param context the context delegated by the invoking contract
     * @param text the text to post on telegram
     * @return response code
     */
    @Override
    public Integer processInvocation(DelegatedContext context, String text) {
        if (token == null) {
            Logger.logInfoMessage("Telegram bot token not specified for message %s", text);
            return -1;
        }
        if (chatId == null) {
            Logger.logInfoMessage("Telegram chat id not specified for message %s", text);
            return -1;
        }
        return sentTelegram(context, text, token, chatId);
    }

    private int sentTelegram(DelegatedContext context, String text, String token, String chatId) {
        String telegramUrl = String.format("https://api.telegram.org/bot%s/sendMessage?text=%s&chat_id=%s", token, text, chatId);
        try {
            URL url = new URL(telegramUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            StringBuilder sb = new StringBuilder();
            InputStream is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                context.logErrorMessage(new Exception(String.format("Response from Telegram API: %s:%s", responseCode, sb.toString())));
            }
            return responseCode;
        } catch (Throwable t) {
            context.logErrorMessage(t);
            return 500;
        }
    }

    public static void main(String[] args) {
        new TelegramNotifier().sentTelegram(null, "test1", "<token>", "<chat id>");
    }
}
