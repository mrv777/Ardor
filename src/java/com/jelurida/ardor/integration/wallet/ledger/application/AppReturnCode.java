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

import java.util.Arrays;

public enum AppReturnCode {
    DEVICE_INTERNAL_ERROR(-1),
    DEVICE_NOT_ENABLED_IN_NODE_CONFIGURATION(101),
    DEVICE_NOT_CONNECTED_OR_LOCKED(102),
    DEVICE_CONNECTED_APP_NOT_OPEN(103),
    WRONG_APP_VERSION(130),
    WRONG_MESSAGE_LENGTH(133),
    RET_BUFFER_TOO_BIG(134),
    WRONG_PADDING(135),
    WRONG_VALUE(136),
    GENERAL_FAILURE(201),
    DEVICE_LOCKED_IN_APP(202),
    NOT_IN_APP(0x6700),
    IN_WRONG_APP(0x6d00);

    private final int code;

    AppReturnCode(int code) {
        this.code = code;
    }

    static AppReturnCode getValue(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst().orElse(null);
    }

    public int getCode() {
        return code;
    }
}
