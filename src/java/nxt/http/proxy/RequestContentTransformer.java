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

import nxt.http.APIProxyServlet;
import nxt.http.JSONResponses;
import nxt.util.PasswordFinder;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;

public class RequestContentTransformer extends BufferingContentTransformer {
    public RequestContentTransformer(HttpServletRequest clientRequest) {
        super(clientRequest);
    }

    @Override
    protected void onContentAvailable(ByteBuffer allInput) {
        if (isConfirmationNeeded()) {
            clientRequest.setAttribute(Attr.POST_DATA, allInput.array());
        }
        int tokenPos = PasswordFinder.process(allInput, APIProxyServlet.PREPROCESSED_SENSITIVE_PARAMS);
        if (tokenPos >= 0) {
            JSONStreamAware error = JSONResponses.PROXY_SECRET_DATA_DETECTED;
            throw new PasswordDetectedException(error);
        }
    }
}
