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

package nxt.http.accountControl;

import nxt.BlockchainTest;
import nxt.addons.JO;
import nxt.blockchain.chaincontrol.PermissionType;
import nxt.http.callers.GetChainPermissionsCall;
import org.json.simple.JSONArray;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.grantPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.removeAllPermissions;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;
import static nxt.util.JSON.jsonArrayCollector;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GetChainPermissionsTest extends BlockchainTest {
    @Before
    public void setUp() {
        generateBlock();

        removeAllPermissions(AEUR);

        grantPermission(AEUR, ALICE, CHAIN_ADMIN);
        grantPermission(AEUR, ALICE, CHAIN_USER);
        grantPermission(AEUR, CHUCK, MASTER_ADMIN);
        grantPermission(AEUR, CHUCK, CHAIN_USER);
    }

    @Test
    public void getChainPermissions() {
        JO actual = GetChainPermissionsCall.create(AEUR.getId()).call();

        assertEquals(list(CHAIN_USER, CHAIN_ADMIN), actual.get(ALICE.getRsAccount()));
        assertEquals(list(CHAIN_USER, MASTER_ADMIN), actual.get(CHUCK.getRsAccount()));
    }

    @Test
    public void getChainPermissionsPaging() {
        JO actual = GetChainPermissionsCall.create(AEUR.getId()).firstIndex(0).lastIndex(1).call();

        assertEquals(list(CHAIN_USER, CHAIN_ADMIN), actual.get(ALICE.getRsAccount()));
        assertNull(actual.get(CHUCK.getRsAccount()));
    }

    static JSONArray list(PermissionType... permissions) {
        return Stream.of(permissions).map(PermissionType::name).collect(jsonArrayCollector());
    }
}