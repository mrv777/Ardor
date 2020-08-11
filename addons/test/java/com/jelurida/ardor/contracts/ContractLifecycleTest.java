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

package com.jelurida.ardor.contracts;

import nxt.tools.ContractManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ContractLifecycleTest extends AbstractContractTest {
    public static final AtomicInteger initCounter = new AtomicInteger();
    public static final AtomicInteger shutdownCounter = new AtomicInteger();

    @Test
    public void lifecycleTest() {
        String contractName = ContractTestHelper.deployContract(LifecycleTestContract.class);
        Assert.assertEquals(1, initCounter.get());
        Assert.assertEquals(0, shutdownCounter.get());
        ContractManager contractManager = new ContractManager();
        contractManager.init(contractName);
        contractManager.delete(contractName);
        generateBlock();
        Assert.assertEquals(1, initCounter.get());
        Assert.assertEquals(1, shutdownCounter.get());
    }
}
