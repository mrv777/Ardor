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
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class DeriveAccountFromSeed extends APIServlet.APIRequestHandler {

    static final DeriveAccountFromSeed instance = new DeriveAccountFromSeed();

    private DeriveAccountFromSeed() {
        super(new APITag[] {APITag.BIP32}, "mnemonic", "passphrase", "bip32Path");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        String mnemonic = Convert.emptyToNull(req.getParameter("mnemonic"));
        if (mnemonic == null) {
            return JSONResponses.missing("mnemonic");
        }
        String passphrase = Convert.nullToEmpty(req.getParameter("passphrase"));
        String bip32Path = Convert.emptyToNull(req.getParameter("bip32Path"));
        return getBip32Node(mnemonic, passphrase, bip32Path);
    }

    /**
     * @param mnemonic typically the 12, 18 or 24 words secret based on BIP39 or a similar words dictionary
     * @param passphrase additional salt to support deriving multiple seeds from the same mnemonic.
     * Not to be confused with an account passphrase which is an unrelated entity.
     * @param bip32Path the bip32 path to the node
     * @return the node information including the privateKey and publicKey which represents the child account key pair
     * used for signing and encryption
     */
    @SuppressWarnings("unchecked")
    private JSONStreamAware getBip32Node(String mnemonic, String passphrase, String bip32Path) {
        JSONObject response = new JSONObject();
        byte[] seed = KeyDerivation.mnemonicToSeed(mnemonic, passphrase);
        response.put("seed", Convert.toHexString(seed));
        if (bip32Path == null) {
            return response;
        }
        KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveSeed(bip32Path, seed);
        byte[] privateKey = bip32Node.getPrivateKeyLeft();
        byte[] privateKeyRight = bip32Node.getPrivateKeyRight();
        byte[] ed25519PublicKey = bip32Node.getMasterPublicKey();
        byte[] serializedMasterPublicKey = bip32Node.getSerializedMasterPublicKey();
        byte[] chainCode = bip32Node.getChainCode();
        byte[] publicKey = bip32Node.getPublicKey();

        response.put("bip32Path", bip32Path);
        response.put("privateKey", Convert.toHexString(privateKey));
        response.put("privateKeyRight", Convert.toHexString(privateKeyRight));
        response.put("masterPublicKey", Convert.toHexString(ed25519PublicKey));
        response.put("serializedMasterPublicKey", Convert.toHexString(serializedMasterPublicKey));
        response.put("chainCode", Convert.toHexString(chainCode));
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
