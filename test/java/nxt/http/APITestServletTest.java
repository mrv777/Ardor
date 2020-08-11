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

package nxt.http;

import nxt.BlockchainTest;
import nxt.util.ResourceLookup;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class APITestServletTest extends BlockchainTest {
    @Test
    public void testPasswordHidingCommonList() throws IOException {
        String actual = requestTestForm("sendMoney");
        assertThat(actual, containsString("<input type='password' name='secretPhrase'"));
        assertThat(actual, not(containsString("<input type='text' name='secretPhrase'")));
    }

    @Test
    public void testCustomSensitiveParameterHiding() throws IOException {
        String handlerName = CustomSensitiveParameterAddOn.handlerName;
        String parameterName = CustomSensitiveParameterAddOn.parameterName;
        String actual = requestTestForm(handlerName);
        assertThat(actual, containsString("<input type='password' name='" + parameterName + "'"));
        assertThat(actual, not(containsString("<input type='text' name='" + parameterName + "'")));
    }

    static String requestTestForm(String apiRequestType) throws IOException {
        URL url = API.getServerRootUri().resolve("/test?requestType=" + apiRequestType).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException(String.format(
                    "code: %s, message: %s",
                    connection.getResponseCode(),
                    connection.getResponseMessage()));
        }
        return new String(ResourceLookup.readInputStream(connection.getInputStream()));
    }
}