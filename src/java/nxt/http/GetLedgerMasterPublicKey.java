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

import com.jelurida.ardor.integration.wallet.ledger.application.ArdorAppBridge;
import com.jelurida.ardor.integration.wallet.ledger.application.ArdorAppInterface;
import com.jelurida.ardor.integration.wallet.ledger.application.PublicKeyData;
import nxt.account.Account;
import nxt.util.Bip32Path;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetLedgerMasterPublicKey extends APIServlet.APIRequestHandler {

    static final GetLedgerMasterPublicKey instance = new GetLedgerMasterPublicKey();

    private GetLedgerMasterPublicKey() {
        super(new APITag[] {APITag.BIP32}, "bip32Path");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        int[] bip32Path = ParameterParser.getBip32Path(req, "bip32Path");
        ArdorAppInterface app = ArdorAppBridge.getApp();
        PublicKeyData publicKeyData = app.getPublicKeyData(bip32Path, true);
        if (publicKeyData.getCurve25519PublicKey().length == 0) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 31);
            response.put("errorDescription", app.getLastError());
            return JSON.prepare(response);
        }
        JSONObject response = new JSONObject();
        response.put("bip32Path", Bip32Path.bip32PathToStr(bip32Path));
        response.put("masterPublicKey", Convert.toHexString(publicKeyData.getEd25519PublicKey()));
        response.put("serializedMasterPublicKey", Convert.toHexString(publicKeyData.getSerializedMasterPublicKey()));
        response.put("chainCode", Convert.toHexString(publicKeyData.getChainCode()));
        response.put("publicKey", Convert.toHexString(publicKeyData.getCurve25519PublicKey()));
        JSONData.putAccount(response, "account", Account.getId(publicKeyData.getCurve25519PublicKey()));
        return response;
    }

    @Override
    protected boolean requirePassword() {
        return true;
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
