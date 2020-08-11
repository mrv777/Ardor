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

import nxt.BlockchainWithChildChainControlTest;
import nxt.Tester;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.blockchain.ChildChain;
import nxt.blockchain.chaincontrol.PermissionType;
import nxt.http.callers.GetAccountPermissionsCall;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.BITSWIFT;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.grantPermission;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;
import static nxt.http.accountControl.GetChainPermissionsTest.list;
import static org.junit.Assert.assertEquals;

public class GetAccountPermissionsTest extends BlockchainWithChildChainControlTest {
    @Before
    public void setUp() {
        generateBlock();

        grantPermission(AEUR, ALICE, CHAIN_ADMIN, CHUCK);
    }

    @Test
    public void testGetAccountPermissionsHasPermissions() {
        assertEquals(
                array(userPermission(CHAIN_USER, ALICE, ALICE, 0)),
                getAccountPermissions(IGNIS).getArray("hasPermissions")
        );
    }

    @Test
    public void testGetAccountPermissionsHasPermissionsContainsAllPermissions() {
        grantPermission(AEUR, ALICE, BLOCKED_CHAIN_ADMIN, CHUCK);

        assertEquals(
                array(userPermission(BLOCKED_CHAIN_ADMIN, ALICE, CHUCK, getHeight()),
                        userPermission(CHAIN_USER, ALICE, ALICE, 0),
                        userPermission(CHAIN_ADMIN, ALICE, CHUCK, getHeight())),
                getAccountPermissions(AEUR).getArray("hasPermissions"));
    }

    @Test
    public void testGetAccountPermissionsHasEffectivePermissions() {
        grantPermission(AEUR, ALICE, BLOCKED_CHAIN_ADMIN, CHUCK);

        assertEquals(
                array(userPermission(BLOCKED_CHAIN_ADMIN, ALICE, CHUCK, getHeight()), userPermission(CHAIN_USER, ALICE, ALICE, 0)),
                getAccountPermissions(AEUR).getArray("hasEffectivePermissions"));
    }

    @Test
    public void testGetAccountPermissionsHasEffectivePermissionsBlocked() {
        grantPermission(AEUR, ALICE, BLOCKED_CHAIN_USER, CHUCK);

        assertEquals(
                array(userPermission(BLOCKED_CHAIN_USER, ALICE, CHUCK, getHeight()), userPermission(CHAIN_ADMIN, ALICE, CHUCK, getHeight())),
                getAccountPermissions(AEUR).getArray("hasEffectivePermissions"));
    }

    @Test
    public void testGetAccountPermissionsHasEffectivePermissionsKeepsUnmatchedBlocks() {
        grantPermission(BITSWIFT, ALICE, BLOCKED_CHAIN_ADMIN, CHUCK);

        assertEquals(
                array(userPermission(BLOCKED_CHAIN_ADMIN, ALICE, CHUCK, getHeight()), userPermission(CHAIN_USER, ALICE, ALICE, 0)),
                getAccountPermissions(BITSWIFT).getArray("hasEffectivePermissions"));
    }

    @Test
    public void testGetPreDefinedAccountPermissions() {
        List<String> defaultAdminAccounts = Arrays.asList("ARDOR-TZ39-8SMJ-U7G4-6M4UF", "ARDOR-2G3B-KBMZ-6KX6-7P4LA");
        for (String account : defaultAdminAccounts) {
            JO actual = GetAccountPermissionsCall.create(IGNIS.getId())
                    .account(account)
                    .call();
            assertEquals(
                    "Actual result: " + actual.toJSONString(),
                    array(userPermission(MASTER_ADMIN, account, 0, -1)),
                    actual.getArray("hasEffectivePermissions"));
        }
    }

    @Test
    public void testGetPreDefinedAccountPermissionsAfterPopOff() {
        blockchainProcessor.popOffTo(0);

        testGetPreDefinedAccountPermissions();
    }

    @Test
    public void testGetAccountPermissionsCanGrant() {
        assertEquals(
                list(CHAIN_USER, BLOCKED_CHAIN_USER),
                getAccountPermissions(AEUR).getArray("canGrantPermissions"));
    }

    @Test
    public void testGetAccountPermissionsCanGrantBasedOnEffectivePermissions() {
        grantPermission(AEUR, ALICE, BLOCKED_CHAIN_ADMIN, CHUCK);

        assertEmpty(getAccountPermissions(AEUR).getArray("canGrantPermissions"));
    }

    private void assertEmpty(JA actual) {
        assertEquals(new JSONArray(), actual);
    }

    private JO getAccountPermissions(ChildChain chain) {
        return GetAccountPermissionsCall.create(chain.getId())
                .account(ALICE.getId())
                .call();
    }

    private static JA array(JO... objects) {
        JA ja = new JA();
        ja.addAllJO(Arrays.asList(objects));
        return ja;
    }

    private static JO userPermission(PermissionType permissionType, Tester user, Tester granter, long height) {
        JO json = new JO();
        json.put("permission", permissionType.name());
        json.put("account", user.getStrId());
        json.put("accountRS", user.getRsAccount());
        json.put("granter", granter.getStrId());
        json.put("granterRS", granter.getRsAccount());
        json.put("height", height);

        return json;
    }

    private static JO userPermission(PermissionType permissionType, String userRs, long granter, long height) {
        JO json = new JO();
        json.put("permission", permissionType.name());
        json.put("account", Long.toUnsignedString(Convert.parseAccountId(userRs)));
        json.put("accountRS", userRs);
        json.put("granter", Long.toUnsignedString(granter));
        json.put("granterRS", Convert.rsAccount(granter));
        json.put("height", height);

        return json;
    }
}