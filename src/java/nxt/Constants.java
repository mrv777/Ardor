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

import nxt.crypto.BIP39;
import nxt.util.Bip32Path;
import nxt.util.Convert;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public final class Constants {

    public static final boolean isTestnet = Nxt.getBooleanProperty("nxt.isTestnet");
    public static final boolean isOffline = Nxt.getBooleanProperty("nxt.isOffline");
    public static final boolean isLightClient = Nxt.getBooleanProperty("nxt.isLightClient");
    public static final boolean isPermissioned = Nxt.getBooleanProperty("nxt.isPermissioned");
    public static final boolean isAutomatedTest = isTestnet && Nxt.getBooleanProperty("nxt.isAutomatedTest");
    public static final boolean isAutomatedTestChildChainPermissions = isTestnet && Nxt.getBooleanProperty("nxt.isAutomatedTestChildChainPermissions");

    public static final String COMPRESSED_SECRET_PHRASE_WORDS;

    public static final int HARDENED = 0x80000000;
    public static final Bip32Path ARDOR_MAINNET_BIP32_ROOT_PATH = Bip32Path.fromString("m/44'/16754'/0'/0'");
    public static final Bip32Path ARDOR_TESTNET_BIP32_ROOT_PATH = Bip32Path.fromString("m/44'/16754'/0'/1'");
    public static final Bip32Path ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH = ARDOR_TESTNET_BIP32_ROOT_PATH.appendChild(0);

    public static Bip32Path getBip32RootPath() {
        return isTestnet ? ARDOR_TESTNET_BIP32_ROOT_PATH : ARDOR_MAINNET_BIP32_ROOT_PATH;
    }

    static {
        String words = String.join(",", BIP39.ENGLISH_WORDLIST);
        COMPRESSED_SECRET_PHRASE_WORDS = Convert.toHexString(Convert.compress(words.getBytes(StandardCharsets.UTF_8)));
    }

    static {
        if (isPermissioned) {
            try {
                Class.forName("com.jelurida.blockchain.authentication.BlockchainRoleMapper");
            } catch (ClassNotFoundException e) {
                throw new ExceptionInInitializerError("BlockchainRoleMapper class required for a permissioned blockchain");
            }
        }
    }

    public static final String ROLE_MAPPING_ACCOUNT_PROPERTY = "ROLE_MAPPING";

    public static final String ACCOUNT_PREFIX = "ARDOR";
    public static final int MAX_NUMBER_OF_FXT_TRANSACTIONS = 10;
    public static final int MAX_NUMBER_OF_CHILD_TRANSACTIONS = 100;
    public static final int MAX_CHILDBLOCK_PAYLOAD_LENGTH = 128 * 1024;
    public static final long EPOCH_BEGINNING;

    static {
        try {
            EPOCH_BEGINNING = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
                    .parse(isTestnet ? "2017-12-26 14:00:00 +0000" : "2018-01-01 00:00:00 +0000").getTime();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final String customLoginWarning = Nxt.getStringProperty("nxt.customLoginWarning", null, false, "UTF-8");

    public static final long MAX_BALANCE_FXT = 1000000000;
    public static final long ONE_FXT = 100000000;
    public static final BigInteger ONE_FXT_BIG_INTEGER = BigInteger.valueOf(ONE_FXT);
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_FXT * ONE_FXT;
    public static final int BLOCK_TIME = 60;
    public static final int TESTNET_ACCELERATION = 6;
    public static final long INITIAL_BASE_TARGET = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(BLOCK_TIME * MAX_BALANCE_FXT)).longValue(); //153722867;
    public static final long MAX_BASE_TARGET = INITIAL_BASE_TARGET * (isTestnet ? MAX_BALANCE_FXT : 50);
    public static final long MIN_BASE_TARGET = INITIAL_BASE_TARGET * 9 / 10;
    public static final int MIN_BLOCKTIME_DELTA = 7;
    public static final int MAX_BLOCKTIME_DELTA = 7;
    public static final int BASE_TARGET_GAMMA = 64;
    public static final int MAX_ROLLBACK = Math.max(Nxt.getIntProperty("nxt.maxRollback"), 720);
    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = isTestnet ? Nxt.getIntProperty("nxt.testnetGuaranteedBalanceConfirmations", 1440) : 1440;
    public static final int LEASING_DELAY = isTestnet ? Nxt.getIntProperty("nxt.testnetLeasingDelay", 1440) : 1440;
    public static final long MIN_FORGING_BALANCE_FQT = 1000 * ONE_FXT;

    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference
    public static final int FORGING_DELAY = Math.min(MAX_TIMEDRIFT - 1, Nxt.getIntProperty("nxt.forgingDelay"));
    public static final int FORGING_SPEEDUP = Nxt.getIntProperty("nxt.forgingSpeedup");
    public static final int DEFAULT_NUMBER_OF_FORK_CONFIRMATIONS = Nxt.getIntProperty(Constants.isTestnet
            ? "nxt.testnetNumberOfForkConfirmations" : "nxt.numberOfForkConfirmations");

    public static final int BATCH_COMMIT_SIZE = Nxt.getIntProperty("nxt.batchCommitSize", Integer.MAX_VALUE);

    public static final byte MAX_PHASING_VOTE_TRANSACTIONS = 10;
    public static final byte MAX_PHASING_WHITELIST_SIZE = 10;
    public static final byte MAX_PHASING_LINKED_TRANSACTIONS = 10;
    public static final int MAX_PHASING_DURATION = 14 * 1440;
    public static final int MAX_PHASING_REVEALED_SECRETS_COUNT = 10;
    public static final int MAX_PHASING_REVEALED_SECRET_LENGTH = 100;
    public static final int MAX_PHASING_COMPOSITE_VOTE_EXPRESSION_LENGTH = 1000;
    public static final int MAX_PHASING_COMPOSITE_VOTE_SUBPOLL_NAME_LENGTH = 10;
    public static final int MAX_PHASING_COMPOSITE_VOTE_VARIABLES_COUNT = 20;
    public static final int MAX_PHASING_COMPOSITE_VOTE_LITERALS_COUNT = 30;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 160;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 160 + 16;

    public static final int MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024;
    public static final int MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH = 42 * 1024;

    public static final int MIN_PRUNABLE_LIFETIME = isTestnet ? 1440 * 60 : 14 * 1440 * 60;
    public static final int MAX_PRUNABLE_LIFETIME;
    public static final boolean ENABLE_PRUNING;

    static {
        int maxPrunableLifetime = Nxt.getIntProperty("nxt.maxPrunableLifetime");
        ENABLE_PRUNING = maxPrunableLifetime >= 0;
        MAX_PRUNABLE_LIFETIME = ENABLE_PRUNING ? Math.max(maxPrunableLifetime, MIN_PRUNABLE_LIFETIME) : Integer.MAX_VALUE;
    }

    public static final boolean INCLUDE_EXPIRED_PRUNABLE = Nxt.getBooleanProperty("nxt.includeExpiredPrunable");
    public static final boolean LEDGER_WALLET_ENABLED = Nxt.getBooleanProperty("nxt.enableLedgerWallet");

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final int MAX_ACCOUNT_PROPERTY_NAME_LENGTH = 32;
    public static final int MAX_ACCOUNT_PROPERTY_VALUE_LENGTH = 160;
    public static final int MAX_ASSET_PROPERTY_NAME_LENGTH = 32;
    public static final int MAX_ASSET_PROPERTY_VALUE_LENGTH = 160;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH = 160;
    public static final int MAX_DIVIDEND_PAYMENT_ROLLBACK = 1441;
    public static final int MIN_DIVIDEND_PAYMENT_INTERVAL = isTestnet ? 3 : 60;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;
    public static final int MAX_POLL_DURATION = 14 * 1440;

    public static final byte MIN_VOTE_VALUE = -92;
    public static final byte MAX_VOTE_VALUE = 92;
    public static final byte NO_VOTE_VALUE = Byte.MIN_VALUE;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 1000;

    public static final int MIN_CURRENCY_NAME_LENGTH = 3;
    public static final int MAX_CURRENCY_NAME_LENGTH = 10;
    public static final int MIN_CURRENCY_CODE_LENGTH = 3;
    public static final int MAX_CURRENCY_CODE_LENGTH = 5;
    public static final int MAX_CURRENCY_DESCRIPTION_LENGTH = 1000;
    public static final long MAX_CURRENCY_TOTAL_SUPPLY = 1000000000L * 100000000L;
    public static final int MAX_MINTING_RATIO = 10000; // per mint units not more than 0.01% of total supply
    public static final byte MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS = 3;
    public static final byte MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS = 30; // max possible at current block payload limit is 51
    public static final short MAX_SHUFFLING_REGISTRATION_PERIOD = (short) 1440 * 7;
    public static final short SHUFFLING_PROCESSING_DEADLINE = (short) (isTestnet ? 10 : 100);
    public static final int SHUFFLER_EXPIRATION_DELAY_BLOCKS = isAutomatedTest ? 1 : 720;

    public static final int MAX_TAGGED_DATA_NAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGGED_DATA_TAGS_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_CHANNEL_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_FILENAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DATA_LENGTH = 42 * 1024;

    public static final int MAX_CONTRACT_NAME_LENGTH = 32;
    public static final int MAX_CONTRACT_PARAMS_LENGTH = 160;

    public static final int MAX_REFERENCED_TRANSACTION_TIMESPAN = 60 * 1440 * 60;
    public static final int CHECKSUM_BLOCK_1 = Constants.isTestnet ? 17000 : 6000;
    public static final int CHECKSUM_BLOCK_2 = Constants.isTestnet ? 230000 : 221000;
    public static final int LIGHT_CONTRACTS_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? 0 : 341500 : 543000;
    public static final int ASSET_PROPERTIES_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? 0 : 455000 : 543000;
    public static final int MPG_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? 1 : 455000 : 543000;
    public static final int CHECKSUM_BLOCK_3 = Constants.isTestnet ? 974000 : 545555;
    public static final int CHECKSUM_BLOCK_4 = Constants.isTestnet ? 1738000 : 695000;
    public static final int CHECKSUM_BLOCK_5 = Constants.isTestnet ? 3214500 : 983000;
    public static final int MISSING_TX_SENDER_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? 0 : 3250000 : 1453000;
    public static final int PERMISSIONED_AEUR_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? 20 : 4949000 : 1453000;
    public static final int CHILD_CHAIN_CONTROL_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? 0 : 4949000 : 1453000;
    public static final int CANCEL_FXT_COIN_ORDER_FIX_BLOCK = Constants.isAutomatedTest ? 20 : CHILD_CHAIN_CONTROL_BLOCK;
    public static final int GPS_BLOCK = Constants.isTestnet ? Constants.isAutomatedTest ? Integer.MAX_VALUE : 4949000 : 1453000;
    public static final long GPS_ASSET_ID = Convert.parseUnsignedLong(Constants.isTestnet ? "8016986501463341146" : "3123987739214429747");

    public static final int LAST_CHECKSUM_BLOCK = CHECKSUM_BLOCK_5;

    public static final int LAST_KNOWN_BLOCK =  Constants.isAutomatedTest ? 0 : CHECKSUM_BLOCK_5;
    public static final long LAST_KNOWN_BLOCK_ID = Convert.parseUnsignedLong(
            isTestnet ? Constants.isAutomatedTest ? "1318911886063902233" : "5657621390974142748" : "11295165462625039807");

    public static final int[] MIN_VERSION = Constants.isTestnet ? new int[] {2, 3, 1} : new int[] {2, 2, 1};
    public static final int[] MIN_PROXY_VERSION = Constants.isTestnet ? new int[] {2, 3, 1} : new int[] {2, 2, 1};

    public static final long UNCONFIRMED_POOL_DEPOSIT_FQT = 10 * ONE_FXT;

    public static final boolean correctInvalidFees = Nxt.getBooleanProperty("nxt.correctInvalidFees");
    public static final int minBundlerBalanceFXT = Nxt.getIntProperty("nxt.minBundlerBalanceFXT");
    public static final int minBundlerFeeLimitFXT = Nxt.getIntProperty("nxt.minBundlerFeeLimitFXT");

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final long BURN_ACCOUNT_ID = Convert.parseAccountId("ARDOR-Q9KZ-74XD-WERK-CV6GB");

    public static final boolean DISABLE_FULL_TEXT_SEARCH = Nxt.getBooleanProperty("nxt.disableFullTextSearch");
    public static final boolean DISABLE_METADATA_DETECTION = Nxt.getBooleanProperty("nxt.disableMetadataDetection");

    private Constants() {
    } // never

}
