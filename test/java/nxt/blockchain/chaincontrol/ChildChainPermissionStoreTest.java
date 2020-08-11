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

package nxt.blockchain.chaincontrol;

import nxt.BlockchainWithChildChainControlTest;
import nxt.dbschema.Db;
import org.junit.Test;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.getChildChainPermissionStore;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ChildChainPermissionStoreTest extends BlockchainWithChildChainControlTest {
    @Test
    public void saveIgnoresExistingPermissionsOverride() {
        ChildChainPermissionStore tested = getChildChainPermissionStore(AEUR);
        Db.db.runInDbTransaction(() -> {
            tested.save(ALICE.getId(), BLOCKED_CHAIN_ADMIN, CHUCK.getId());
            tested.save(ALICE.getId(), BLOCKED_CHAIN_ADMIN, DAVE.getId());

            ChildChainPermission actual = tested.get(ALICE.getId(), BLOCKED_CHAIN_ADMIN);
            assertEquals(DAVE.getId(), actual.getGranterId());
        });
    }

    @Test
    public void popOfRevertsDelete() {
        generateBlock();
        ChildChainPermissionStore tested = getChildChainPermissionStore(IGNIS);

        Db.db.runInDbTransaction(() -> tested.remove(ALICE.getId(), CHAIN_USER));

        assertNull(tested.get(ALICE.getId(), CHAIN_USER));

        blockchainProcessor.popOffTo(baseHeight);

        assertNotNull(tested.get(ALICE.getId(), CHAIN_USER));
    }
}