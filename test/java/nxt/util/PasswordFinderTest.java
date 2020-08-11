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

package nxt.util;

import nxt.http.APIProxyServlet;
import nxt.util.PasswordFinder;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class PasswordFinderTest {
    @Test
    public void passwordFinder() {
        ByteBuffer postData = ByteBuffer.wrap("abcsecretPhrase=def".getBytes());
        Assert.assertEquals(3, PasswordFinder.process(postData, "secretPhrase="));
        postData.rewind();
        Assert.assertEquals(-1, PasswordFinder.process(postData, "mySecret="));
        postData.rewind();
        Assert.assertEquals(3, PasswordFinder.process(postData, "mySecret=", "secretPhrase="));
        postData.rewind();
        Assert.assertEquals(0, PasswordFinder.process(postData, "secretPhrase=", "abc"));
        postData.rewind();
        Assert.assertEquals(16, PasswordFinder.process(postData, "def"));
    }

    @Test
    public void testCollectedSensitiveParameters() {
        Assert.assertEquals(3, PasswordFinder.process(
                ByteBuffer.wrap("abcsecretPhrase=def".getBytes()), APIProxyServlet.PREPROCESSED_SENSITIVE_PARAMS));

        Assert.assertEquals(4, PasswordFinder.process(
                ByteBuffer.wrap("b=a&passphrase=xxx".getBytes()), APIProxyServlet.PREPROCESSED_SENSITIVE_PARAMS));

        Assert.assertEquals(0, PasswordFinder.process(
                ByteBuffer.wrap("sharedKey=xxx".getBytes()), APIProxyServlet.PREPROCESSED_SENSITIVE_PARAMS));

        Assert.assertEquals(3, PasswordFinder.process(
                ByteBuffer.wrap("c=bencryptionPassword=xxx".getBytes()), APIProxyServlet.PREPROCESSED_SENSITIVE_PARAMS));
    }
}
