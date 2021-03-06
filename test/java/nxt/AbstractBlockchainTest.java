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

package nxt;

import nxt.blockchain.Block;
import nxt.blockchain.BlockchainImpl;
import nxt.blockchain.BlockchainProcessor;
import nxt.blockchain.BlockchainProcessorImpl;
import nxt.blockchain.Generator;
import nxt.blockchain.TransactionProcessorImpl;
import nxt.configuration.Setup;
import nxt.crypto.Crypto;
import nxt.util.Listener;
import nxt.util.Logger;
import org.junit.Assert;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public abstract class AbstractBlockchainTest {

    protected static BlockchainProcessorImpl blockchainProcessor;
    protected static BlockchainImpl blockchain;
    private static final Object doneLock = new Object();
    private static boolean done = false;

    protected static Properties newTestProperties() {
        Properties testProperties = new Properties();
        testProperties.setProperty("nxt.shareMyAddress", "false");
        testProperties.setProperty("nxt.savePeers", "false");
        //testProperties.setProperty("nxt.enableAPIServer", "false");
        //testProperties.setProperty("nxt.enableUIServer", "false");
        testProperties.setProperty("nxt.disableGenerateBlocksThread", "true");
        //testProperties.setProperty("nxt.disableProcessTransactionsThread", "true");
        //testProperties.setProperty("nxt.disableRemoveUnconfirmedTransactionsThread", "true");
        //testProperties.setProperty("nxt.disableRebroadcastTransactionsThread", "true");
        //testProperties.setProperty("nxt.disablePeerUnBlacklistingThread", "true");
        //testProperties.setProperty("nxt.getMorePeers", "false");
        testProperties.setProperty("nxt.testUnconfirmedTransactions", "true");
        testProperties.setProperty("nxt.debugTraceAccounts", "");
        testProperties.setProperty("nxt.debugLogUnconfirmed", "false");
        testProperties.setProperty("nxt.debugTraceQuote", "\"");
        testProperties.setProperty("nxt.runtime.mode", "");
        //testProperties.setProperty("nxt.numberOfForkConfirmations", "0");
        return testProperties;
    }

    private static boolean isCause(Class<? extends Throwable> expected, Throwable exc) {
        return expected.isInstance(exc)
                || (exc != null && isCause(expected, exc.getCause()));
    }

    protected static void init(Properties testProperties) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try {
                Nxt.init(Setup.UNIT_TEST, testProperties);
            } catch (Throwable t) {
                if (isCause(AccessControlException.class, t)) {
                    throw new AssertionError("Initialization failed due to denied access.\n" +
                            "The cause is most probably that the compiled classes don't reside in the directories" +
                            " required by ardor.policy.\n" +
                            "Make sure that you IDE outputs the classes in same directories as the compile script", t);
                } else {
                    throw t;
                }
            }
            return null;
        });
        blockchain = BlockchainImpl.getInstance();
        blockchainProcessor = BlockchainProcessorImpl.getInstance();
        blockchainProcessor.setGetMoreBlocks(false);
        TransactionProcessorImpl.getInstance().clearUnconfirmedTransactions();
        Listener<Block> countingListener = block -> {
            if (block.getHeight() % 1000 == 0) {
                Logger.logMessage("downloaded block " + block.getHeight());
            }
        };
        blockchainProcessor.addListener(countingListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    protected static void shutdown() {
        TransactionProcessorImpl.getInstance().clearUnconfirmedTransactions();
    }

    protected static void downloadTo(final int endHeight) {
        if (blockchain.getHeight() == endHeight) {
            return;
        }
        Assert.assertTrue(blockchain.getHeight() < endHeight);
        Listener<Block> stopListener = block -> {
            if (blockchain.getHeight() == endHeight) {
                synchronized (doneLock) {
                    done = true;
                    blockchainProcessor.setGetMoreBlocks(false);
                    doneLock.notifyAll();
                    throw new NxtException.StopException("Reached height " + endHeight);
                }
            }
        };
        blockchainProcessor.addListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
        synchronized (doneLock) {
            done = false;
            Logger.logMessage("Starting download from height " + blockchain.getHeight());
            blockchainProcessor.setGetMoreBlocks(true);
            while (! done) {
                try {
                    doneLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Assert.assertEquals(endHeight, blockchain.getHeight());
        blockchainProcessor.removeListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    protected static void forgeTo(final int endHeight, final String secretPhrase) {
        if (blockchain.getHeight() == endHeight) {
            return;
        }
        Assert.assertTrue(blockchain.getHeight() < endHeight);
        Listener<Block> stopListener = block -> {
            if (blockchain.getHeight() == endHeight) {
                synchronized (doneLock) {
                    done = true;
                    Generator.stopForging(Crypto.getPrivateKey(secretPhrase));
                    doneLock.notifyAll();
                }
            }
        };
        blockchainProcessor.addListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
        synchronized (doneLock) {
            done = false;
            Logger.logMessage("Starting forging from height " + blockchain.getHeight());
            Generator.startForging(Crypto.getPrivateKey(secretPhrase));
            while (! done) {
                try {
                    doneLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Assert.assertTrue(blockchain.getHeight() >= endHeight);
        Assert.assertArrayEquals(Crypto.getPublicKey(Crypto.getPrivateKey(secretPhrase)), blockchain.getLastBlock().getGeneratorPublicKey());
        blockchainProcessor.removeListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    protected int getHeight() {
        return blockchain.getHeight();
    }
}
