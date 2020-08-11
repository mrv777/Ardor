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

package nxt.addons;

import nxt.Constants;
import nxt.account.Account;
import nxt.blockchain.Generator;
import nxt.crypto.Crypto;
import nxt.http.APITag;
import nxt.http.ParameterException;
import nxt.http.ParameterParser;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForgingEncryptedConfig extends AbstractEncryptedConfig {

    public static final String CONFIG_FILE_NAME = "forgers";

    @Override
    protected String getAPIRequestName() {
        return "Forging";
    }

    @Override
    protected APITag getAPITag() {
        return APITag.FORGING;
    }

    @Override
    protected String getDataParameter() {
        return "passphrases";
    }

    @Override
    protected JSONStreamAware processDecrypted(BufferedReader reader) throws IOException {
        int count = 0;
        long forgingBalance = 0;
        String line;
        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            // Here we don't know if the lines in this file represent secret phrases since the file was generated by an old version,
            // or private keys generated with a newer version, therefore we need to use some heuristics like we do in the wallet.
            String secret = line.trim();
            byte[] privateKey = Convert.EMPTY_BYTE;
            if (secret.length() == 64) {
                try {
                    privateKey = Convert.parseHexString(secret);
                } catch (Exception e) {
                    // the line is not really a private just happens to be 64 chars
                }
            }
            if (privateKey.length == 0) {
                String prefix = "Passphrase:";
                if (secret.startsWith(prefix) && secret.length() == prefix.length() + 64) {
                    privateKey = Crypto.getPrivateKey(secret.substring(prefix.length(), prefix.length() + 64));
                } else {
                    privateKey = Crypto.getPrivateKey(secret);
                }
            }
            Generator.startForging(privateKey);
            byte[] publicKey = Crypto.getPublicKey(privateKey);
            Account account = Account.getAccount(publicKey);
            if (account == null) {
                Logger.logWarningMessage("Forge request in startForgingEncrypted for nonexistent account " + Convert.toHexString(publicKey));
            } else {
                forgingBalance += account.getEffectiveBalanceFXT();
            }
            count++;
        }
        JSONObject response = new JSONObject();
        response.put("forgersStarted", count);
        response.put("totalEffectiveBalance", String.valueOf(forgingBalance));
        return response;
    }

    @Override
    protected List<String> getExtraParameters() {
        return Collections.singletonList("minEffectiveBalanceFXT");
    }

    @Override
    protected String getSaveData(HttpServletRequest request) throws ParameterException {
        String passphrases = ParameterParser.getParameter(request, "passphrases");
        long minEffectiveBalanceFXT = ParameterParser.getLong(request, "minEffectiveBalanceFXT", 0, Constants.MAX_BALANCE_FXT, false);
        StringWriter stringWriter = new StringWriter();
        try (BufferedReader reader = new BufferedReader(new StringReader(passphrases));
             BufferedWriter writer = new BufferedWriter(stringWriter)) {
            Set<Long> accountIds = new HashSet<>();
            String passphrase;
            while ((passphrase = reader.readLine()) != null) {
                Account account = Account.getAccount(Crypto.getPublicKey(Crypto.getPrivateKey(passphrase)));
                if (account != null && account.getEffectiveBalanceFXT() >= minEffectiveBalanceFXT && accountIds.add(account.getId())) {
                    writer.write(passphrase);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return stringWriter.toString();
    }

    @Override
    protected String getDefaultFilename() {
        return CONFIG_FILE_NAME;
    }
}
