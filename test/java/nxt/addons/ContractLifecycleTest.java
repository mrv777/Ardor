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

import org.junit.Assert;
import org.junit.Test;

public class ContractLifecycleTest {
    private static class LifecycleTestContract extends AbstractContract<Void,Void> {
        private int initCounter = 0;
        private int shutdownCounter = 0;

        @Override
        public void init(InitializationContext context) {
            ++initCounter;
        }

        @Override
        public void shutdown(ShutdownContext context) {
            ++shutdownCounter;
        }
    }

    @Test
    public void noInit() {
        LifecycleTestContract contract = new LifecycleTestContract();
        new ContractAndSetupParameters("test", contract, null);
        Assert.assertEquals(0, contract.initCounter);
        Assert.assertEquals(0, contract.shutdownCounter);
    }

    @Test
    public void initOnce() {
        LifecycleTestContract contract = new LifecycleTestContract();
        ContractAndSetupParameters contractAndSetupParameters = new ContractAndSetupParameters("test", contract, null);
        contractAndSetupParameters.init(() -> null);
        Assert.assertEquals(1, contract.initCounter);
        Assert.assertEquals(0, contract.shutdownCounter);
    }

    @Test
    public void multipleInit() {
        LifecycleTestContract contract = new LifecycleTestContract();
        ContractAndSetupParameters contractAndSetupParameters = new ContractAndSetupParameters("test", contract, null);
        for(int i = 0; i < 5; i++) {
            contractAndSetupParameters.init(() -> null);
        }
        Assert.assertEquals(1, contract.initCounter);
        Assert.assertEquals(0, contract.shutdownCounter);
    }

    @Test
    public void initAndShutdown() {
        LifecycleTestContract contract = new LifecycleTestContract();
        ContractAndSetupParameters contractAndSetupParameters = new ContractAndSetupParameters("test", contract, null);
        contractAndSetupParameters.init(() -> null);
        contractAndSetupParameters.shutdown(() -> null);
        Assert.assertEquals(1, contract.initCounter);
        Assert.assertEquals(1, contract.shutdownCounter);
    }

    @Test
    public void onlyShutdown() {
        LifecycleTestContract contract = new LifecycleTestContract();
        ContractAndSetupParameters contractAndSetupParameters = new ContractAndSetupParameters("test", contract, null);
        contractAndSetupParameters.shutdown(() -> null);
        Assert.assertEquals(0, contract.initCounter);
        Assert.assertEquals(0, contract.shutdownCounter);
    }

    @Test
    public void initAndMultipleShutdown() {
        LifecycleTestContract contract = new LifecycleTestContract();
        ContractAndSetupParameters contractAndSetupParameters = new ContractAndSetupParameters("test", contract, null);
        contractAndSetupParameters.init(() -> null);
        for(int i = 0; i < 5; i++) {
            contractAndSetupParameters.shutdown(() -> null);
        }
        Assert.assertEquals(1, contract.initCounter);
        Assert.assertEquals(1, contract.shutdownCounter);
    }
}
