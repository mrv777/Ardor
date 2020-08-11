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

package nxt.http;

import nxt.account.Account;
import nxt.crypto.KeyDerivation;
import nxt.crypto.SerializedMasterPublicKey;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class DeriveAccountFromMasterPublicKey extends APIServlet.APIRequestHandler {

    static final DeriveAccountFromMasterPublicKey instance = new DeriveAccountFromMasterPublicKey();

    private DeriveAccountFromMasterPublicKey() {
        super(new APITag[] {APITag.BIP32}, "serializedMasterPublicKey", "childIndex");
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        SerializedMasterPublicKey serializedMasterPublicKey = ParameterParser.getSerializedMasterPublicKey(req, "serializedMasterPublicKey", true);
        int childIndex = ParameterParser.getInt(req, "childIndex", 0, (int)Math.pow(2, 16) - 1, true);
        KeyDerivation.Bip32Node bip32NodeData = KeyDerivation.deriveChildPublicKey(serializedMasterPublicKey, childIndex);
        byte[] publicKey = bip32NodeData.getPublicKey();

        JSONObject response = new JSONObject();
        response.put("publicKey", Convert.toHexString(publicKey));
        JSONData.putAccount(response, "account", Account.getId(publicKey));
        return response;
    }

    @Override
    protected boolean requirePassword() {
        return false;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected boolean isChainSpecific() {
        return false;
    }
}
