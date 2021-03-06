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

import java.util.List;

public interface TaggedDataResponse {
    static TaggedDataResponse create(JO object) {
        return new TaggedDataResponseImpl(object);
    }

    static TaggedDataResponse create(JSONObject object) {
        return new TaggedDataResponseImpl(object);
    }

    byte[] getTransactionFullHash();

    long getAccount();

    String getName();

    String getDescription();

    String getTags();

    List<String> getParsedTags();

    String getType();

    String getChannel();

    String getFilename();

    boolean isText();

    byte[] getData();

    int getTransactionTimestamp();

    int getBlockTimestamp();
}
