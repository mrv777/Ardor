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

import com.jelurida.ardor.contracts.trading.CoinExchangeTradingBot;
import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.addons.BlockContext;
import nxt.addons.CloudDataClassLoader;
import nxt.addons.Contract;
import nxt.addons.ContractLoader;
import nxt.addons.ContractRunnerConfig;
import nxt.addons.NullContractRunnerConfig;
import nxt.blockchain.ChainTransactionId;
import nxt.tools.ContractManager;
import nxt.util.security.BlockchainPermission;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ContractLoaderTest extends AbstractContractTest {

    @BeforeClass
    public static void init() {
        Map<String, String> properties = new HashMap<>();
        properties.put("nxt.addOns", "nxt.addons.ContractRunner");
        properties.put("addon.contractRunner.secretPhrase", BlockchainTest.aliceSecretPhrase);
        properties.put("addon.contractRunner.feeRateNQTPerFXT.IGNIS", "200000000");
        properties.put("nxt.testnetLeasingDelay", "2");
        properties.put("contract.manager.secretPhrase", aliceSecretPhrase);
        properties.put("contract.manager.feeNQT", "100000000");
        properties.put("contract.manager.serverAddress", "");
        initNxt(properties);
        initBlockchainTest();
    }

    @Test
    public void loadHelloWorld() {
        Class<? extends Contract> contractClass = HelloWorld.class;
        Contract<?, ?> contract = loadContract((Class<Contract<?,?>>)contractClass);
        Assert.assertEquals(HelloWorld.class.getName(), contract.getClass().getName());
        Assert.assertTrue(contract.getClass().getClassLoader() instanceof CloudDataClassLoader);
        AccessControlContext context = AccessController.getContext();
        context.checkPermission(new BlockchainPermission("*"));
    }

    @Test
    public void loadTradingBot() {
        Class<? extends Contract> contractClass = CoinExchangeTradingBot.class;
        Contract<?, ?> contract = loadContract((Class<Contract<?,?>>)contractClass);
        Class<?>[] declaredClasses = contract.getClass().getDeclaredClasses();
        Arrays.stream(declaredClasses).forEach(declaredClass -> Assert.assertTrue(declaredClass.getClassLoader() instanceof CloudDataClassLoader));
    }

    @Test
    public void loadForbiddenActions() {
        Class<? extends Contract> contractClass = ForbiddenActions.class;
        Contract<?, ?> contract = loadContract((Class<Contract<?,?>>)contractClass);
        Assert.assertEquals(ForbiddenActions.class.getName(), contract.getClass().getName());
        Assert.assertTrue(contract.getClass().getClassLoader() instanceof CloudDataClassLoader);
        ContractRunnerConfig config = new NullContractRunnerConfig("") {
            public String getAccountRs() {
                return ALICE.getRsAccount();
            }
        };
        BlockContext blockContext = new BlockContext(Nxt.getBlockchain().getBlockAtHeight(2), config, "ForbiddenActions");
        contract.processBlock(blockContext);
    }

    private Contract<?, ?> loadContract(Class<? extends Contract<?,?>> contractClass) {
        ContractManager contractManager = new ContractManager();
        String contractName = contractClass.getSimpleName();
        contractManager.init(contractName);
        ContractManager.ContractData contractData = contractManager.uploadImpl(contractName, contractClass.getPackage().getName());
        generateBlock();
        ChainTransactionId id = new ChainTransactionId(2, contractData.getResponse().parseHexString("fullHash"));
        return ContractLoader.loadContract(id);
    }
}
