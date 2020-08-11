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

package nxt.ae;

import nxt.Constants;
import nxt.account.Account.AccountAsset;
import nxt.account.HoldingType;
import nxt.db.DbIterator;
import nxt.migration.HoldingSnapshot;
import nxt.util.Convert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

class AssetSnapshot extends HoldingSnapshot {

    private static final Map<Long, Set<Long>> excluded = new HashMap<>();
    static {
        excluded.put(Constants.GPS_ASSET_ID, new HashSet<>(Constants.isTestnet ?
                Arrays.asList(Convert.parseAccountId("ARDOR-SZKV-J8TH-GSM9-9LKV6"), Convert.parseAccountId("ARDOR-E93F-7E8Z-BHJ8-A65RG"))
                :
                Arrays.asList(Convert.parseAccountId("ARDOR-N7EU-TSD7-U28W-DZTKH"), Convert.parseAccountId("ARDOR-5KLU-SZDN-H9YS-ET629"))));
    }

    AssetSnapshot() {
        super(HoldingType.ASSET);
    }

    @Override
    protected Map<String, Long> takeSnapshot(long holdingId) {
        final Set<Long> exclude = excluded.get(holdingId);
        Asset asset = Asset.getAsset(holdingId);
        String issuerAccount = Long.toUnsignedString(asset.getAccountId());
        Predicate<AccountAsset> filter = exclude == null ? accountAsset -> true : accountAsset -> !exclude.contains(accountAsset.getAccountId());
        try (DbIterator<AccountAsset> accountAssets = asset.getAccounts(0, -1)) {
            Map<String, Long> snapshot = new HashMap<>();
            long excludedQuantityQNT = 0;
            while (accountAssets.hasNext()) {
                AccountAsset accountAsset = accountAssets.next();
                if (filter.test(accountAsset)) {
                    snapshot.put(Long.toUnsignedString(accountAsset.getAccountId()), accountAsset.getQuantityQNT());
                } else {
                    excludedQuantityQNT += accountAsset.getQuantityQNT();
                }
            }
            snapshot.put(issuerAccount, excludedQuantityQNT + Convert.nullToZero(snapshot.get(issuerAccount)));
            return snapshot;
        }
    }
}
