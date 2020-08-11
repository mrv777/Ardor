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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfirmationReport {
    private final String requestType;
    private final int timestamp;
    private final List<URI> confirmingNodes = new CopyOnWriteArrayList<>();
    private final List<URI> rejectingNodes = new CopyOnWriteArrayList<>();

    public ConfirmationReport(String requestType, int timestamp) {
        this.requestType = requestType;
        this.timestamp = timestamp;
    }

    public String getRequestType() {
        return requestType;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public List<URI> getConfirmingNodes() {
        return Collections.unmodifiableList(confirmingNodes);
    }

    public List<URI> getRejectingNodes() {
        return Collections.unmodifiableList(rejectingNodes);
    }

    void addConfirmingNode(URI nodeUrl) {
        confirmingNodes.add(nodeUrl);
    }

    void addRejectingNode(URI nodeUrl) {
        rejectingNodes.add(nodeUrl);
    }
}
