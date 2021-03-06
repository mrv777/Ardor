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

package nxt.blockchain;

import nxt.account.HoldingType;
import nxt.migration.HoldingSnapshot;

import java.util.Map;

class ChildChainSnapshot extends HoldingSnapshot {

    ChildChainSnapshot() {
        super(HoldingType.COIN);
    }

    @Override
    protected Map<String, Long> takeSnapshot(long holdingId) {
        throw new UnsupportedOperationException("Snapshot of child chain balances not supported");
    }
}
