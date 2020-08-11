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

package nxt.addons;

import nxt.http.APITag;
import org.json.simple.JSONStreamAware;

import java.io.BufferedReader;

public class ContractRunnerEncryptedConfig extends AbstractEncryptedConfig {
    private final ContractRunner contractRunner;

    ContractRunnerEncryptedConfig(ContractRunner contractRunner) {
        this.contractRunner = contractRunner;
    }

    @Override
    protected String getAPIRequestName() {
        return "ContractRunner";
    }

    @Override
    protected APITag getAPITag() {
        return null;
    }

    @Override
    protected String getDataParameter() {
        return "contractRunner";
    }

    @Override
    protected JSONStreamAware processDecrypted(BufferedReader reader) {
        return contractRunner.parseConfig(reader);
    }
}
