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

public class Attr {
    public static final String REMOTE_URL = Attr.class.getName() + ".remoteUrl";
    public static final String REMOTE_SERVER_IDLE_TIMEOUT = Attr.class.getName() + ".remoteServerIdleTimeout";
    public static final String REQUEST_TYPE = Attr.class.getName() + ".requestType";
    public static final String REQUEST_NEEDS_CONFIRMATION = Attr.class.getName() + ".requestNeedsConfirmation";
    public static final String POST_DATA = Attr.class.getName() + ".postData";
    public static final String RESPONSE_CONTENT = Attr.class.getName() + ".responseContent";
}
