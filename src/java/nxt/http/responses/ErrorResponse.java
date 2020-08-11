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

package nxt.http.responses;

import nxt.addons.JO;
import org.json.simple.JSONObject;

public interface ErrorResponse {

    static ErrorResponse create(JO object) {
        if (object.isExist("errorCode")) {
            return new ErrorResponseImpl(object);
        } else {
            return NO_ERROR;
        }
    }

    static ErrorResponse create(JSONObject object) {
        return create(new JO(object));
    }

    int getErrorCode();

    String getErrorDescription();

    boolean isError();

    ErrorResponse NO_ERROR = new ErrorResponse() {
        @Override
        public int getErrorCode() {
            return 0;
        }

        @Override
        public String getErrorDescription() {
            return null;
        }

        @Override
        public boolean isError() {
            return false;
        }
    };

}
