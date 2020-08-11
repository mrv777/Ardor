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
package nxt.http.proxy;

import nxt.Nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ReportsManager {
    public static final int CONFIRMATION_REPORTS_EXPIRATION_SECONDS = 75;

    private final ConcurrentLinkedQueue<ConfirmationReport> confirmationReports = new ConcurrentLinkedQueue<>();
    private int totalConfirmations = 0;
    private int totalRejections = 0;

    public ReportsManager() {
        ResponseConfirmation.addListener(this::onConfirmation,
                ResponseConfirmation.Event.CONFIRMATION);
        ResponseConfirmation.addListener(this::onRejection,
                ResponseConfirmation.Event.REJECTION);
    }

    private synchronized void onConfirmation(ResponseConfirmation confirmation) {
        totalConfirmations++;
    }

    private synchronized void onRejection(ResponseConfirmation confirmation) {
        totalRejections++;
    }

    public void addReport(ConfirmationReport report) {
        confirmationReports.offer(report);

        int expiredReportsTime = Nxt.getEpochTime() - CONFIRMATION_REPORTS_EXPIRATION_SECONDS;
        synchronized (this) {
            for (Iterator<ConfirmationReport> it = confirmationReports.iterator(); it.hasNext(); ) {
                ConfirmationReport removedReport = it.next();
                if (removedReport.getTimestamp() < expiredReportsTime) {
                    it.remove();
                    totalConfirmations -= removedReport.getConfirmingNodes().size();
                    totalRejections -= removedReport.getRejectingNodes().size();
                } else {
                    break;
                }
            }
        }
    }

    public Collection<ConfirmationReport> getConfirmationReports() {
        return Collections.unmodifiableCollection(confirmationReports);
    }

    public int getTotalConfirmations() {
        return totalConfirmations;
    }

    public int getTotalRejections() {
        return totalRejections;
    }
}
