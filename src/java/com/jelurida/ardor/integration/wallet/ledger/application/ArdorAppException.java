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

package com.jelurida.ardor.integration.wallet.ledger.application;

import nxt.util.Convert;

public class ArdorAppException extends RuntimeException {

    int reasonCode;
    byte[] data;

    public ArdorAppException(int reasonCode, byte[] data) {
        this.reasonCode = reasonCode;
        this.data = data;
    }

    public ArdorAppException(AppReturnCode reason) {
        this(reason, Convert.EMPTY_BYTE);
    }

    public ArdorAppException(AppReturnCode reason, byte[] data) {
        this(reason.getCode(), data);
    }

    @Override
    public String getMessage() {
        return String.format("reason %d %s length %d data %s", reasonCode, AppReturnCode.getValue(reasonCode), data.length, Convert.toHexString(data));
    }

    @Override
    public String getLocalizedMessage() {
        return AppReturnCode.getValue(reasonCode).toString(); // this is a message that the wallet can localize
    }
}
