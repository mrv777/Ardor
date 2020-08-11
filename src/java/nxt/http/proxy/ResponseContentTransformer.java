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

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;

public class ResponseContentTransformer extends BufferingContentTransformer {
    public ResponseContentTransformer(HttpServletRequest clientRequest) {
        super(clientRequest);
    }

    @Override
    protected void onContentAvailable(ByteBuffer allInput) {
        if (isConfirmationNeeded()) {
            ComparableResponse responseContent = new ComparableResponse(
                    (String) clientRequest.getAttribute(Attr.REQUEST_TYPE), allInput.array());
            clientRequest.setAttribute(Attr.RESPONSE_CONTENT, responseContent);
        }
    }
}
