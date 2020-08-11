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

package nxt.addons;

import nxt.account.AccountLedger;
import nxt.util.Convert;
import nxt.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public abstract class AbstractLedgerLogger implements AddOn {

    protected String getFilename() {
        return "ledgerlog.csv";
    }

    protected abstract String getColumnNames();

    protected abstract String getLogLine(AccountLedger.LedgerEntry ledgerEntry);

    private PrintWriter writer = null;

    @Override
    public void init() {
        try {
            writer = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getFilename())))), true);
            writer.println(getColumnNames());
            AccountLedger.addListener(ledgerEntry -> {
                String logLine = getLogLine(ledgerEntry);
                if (Convert.emptyToNull(logLine) != null) {
                    writer.println(logLine);
                }
            }, AccountLedger.Event.ADD_ENTRY);
        } catch (IOException e) {
            Logger.logErrorMessage(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

}
