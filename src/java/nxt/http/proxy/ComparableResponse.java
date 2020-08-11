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

import nxt.messaging.PrunableEncryptedMessageAppendix;
import nxt.messaging.PrunablePlainMessageAppendix;
import nxt.shuffling.ShufflingTransactionType;
import nxt.taggeddata.TaggedDataTransactionType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ComparableResponse {
    private final String requestType;
    private final Object json;

    private final static Set<String> PRUNABLE_ATTACHMENT_KEYS = Stream.of(
            PrunablePlainMessageAppendix.appendixName,
            PrunableEncryptedMessageAppendix.appendixName,
            ShufflingTransactionType.SHUFFLING_PROCESSING.getName(),
            TaggedDataTransactionType.TAGGED_DATA_UPLOAD.getName()
    ).map(s -> "version." + s).collect(Collectors.toSet());

    public ComparableResponse(String requestType, byte[] bytes) {
        this.requestType = requestType;
        json = JSONValue.parse(new InputStreamReader(new ByteArrayInputStream(bytes)));
        if (json instanceof JSONObject) {
            transformToComparableResponse(requestType, (JSONObject) json);
        }
    }

    private void transformToComparableResponse(String requestType, JSONObject response) {
        response.remove("requestProcessingTime");
        response.remove("confirmations");
        if ("getBlock".equals(requestType)) {
            response.remove("nextBlock");
        } else if (response.containsKey("transactions")) {
            JSONArray transactions = (JSONArray) response.get("transactions");
            for (Object t : transactions) {
                JSONObject transaction = (JSONObject) t;
                transaction.remove("confirmations");
                normalizePrunableAttachment(transaction);
            }
        } else if ("getAccountLedger".equals(requestType)) {
            JSONArray entries = (JSONArray) response.get("entries");
            if (entries != null) {
                for (Object e : entries) {
                    JSONObject entry = (JSONObject) e;
                    entry.remove("ledgerId");
                }
            }
        }
    }

    private void normalizePrunableAttachment(JSONObject transaction) {
        JSONObject attachment = (JSONObject) transaction.get("attachment");
        if (attachment != null) {
            boolean isPrunableAttachment = !Collections.disjoint(attachment.keySet(), PRUNABLE_ATTACHMENT_KEYS);
            if (!isPrunableAttachment) {
                return;
            }
            attachment.entrySet().removeIf(e -> {
                String key = (String) ((Map.Entry) e).getKey();
                return key.length() < 4 || !"hash".equals(key.substring(key.length() - 4).toLowerCase(Locale.ROOT));
            });
        }
    }

    public boolean isConfirming(ComparableResponse other) {
        if ("getAccountLedger".equals(requestType)) {
            return compareLedgerEntries((JSONObject)this.json, (JSONObject)other.json);
        } else {
            return this.json.equals(other.json);
        }
    }

    private boolean compareLedgerEntries(JSONObject obj1, JSONObject obj2) {
        Object e1 = obj1.get("entries");
        Object e2 = obj2.get("entries");
        if (e1 == e2) {
            return true;
        }

        if (e1 instanceof JSONArray && e2 instanceof JSONArray) {
            JSONArray entries1 = (JSONArray) e1;
            JSONArray entries2 = (JSONArray) e2;
            for (int i = 0; i < entries1.size() && i < entries2.size(); i++) {
                if (!Objects.equals(entries1.get(i), entries2.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ComparableResponse{" +
                "requestType='" + requestType + '\'' +
                ", json=" + json +
                '}';
    }
}
