package com.jelurida.ardor.contracts.trading;

import com.jelurida.ardor.contracts.AbstractContractTest;
import com.jelurida.ardor.contracts.SlackNotifier;
import com.jelurida.ardor.contracts.TestApiAddOn.CollectMessagesRule;
import nxt.Nxt;
import nxt.Tester;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.blockchain.ChildChain;
import nxt.http.callers.SendMoneyCall;
import nxt.http.callers.TransferAssetCall;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static com.jelurida.ardor.contracts.ContractTestHelper.deployContract;
import static com.jelurida.ardor.contracts.trading.CoinExchangeTradingBotTest.createAndDistributeAsset;
import static java.util.Collections.emptyList;
import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static org.junit.Assert.assertEquals;

public class AccountBalanceNotifierTest extends AbstractContractTest {
    @Rule
    public final CollectMessagesRule emulatedSlackMessages = new CollectMessagesRule(); // Updated by calls from the contract to slack which triggers the TestApiAddOn

    @Before
    public void setUp() {
        assertEquals(1, getHeight());
    }

    @Test
    public void coinEmpty() {
        deployContract(AccountBalanceNotifier.class);
        deployContract(SlackNotifier.class);

        sendCoinToBob(ALICE, IGNIS);

        generateBlock();
        JO message = new JO();
        message.put("text", String.format(AccountBalanceNotifier.ALERT_MESSAGE_FORMAT, "ARDOR-XK4R-7VJU-6EQG-7R335", "IGNIS", "coin", "1.00000000", "7.00000000", Nxt.getBlockchain().getHeight()));
        assertEquals(message.toJSONString(), emulatedSlackMessages.getMessages().get(0));
    }

    @Test
    public void multipleCoinsEmpty() {
        deployContract(AccountBalanceNotifier.class);
        deployContract(SlackNotifier.class);

        sendCoinToBob(ALICE, IGNIS);
        sendCoinToBob(DAVE, IGNIS);

        generateBlock();
        assertEquals(2, emulatedSlackMessages.getMessages().size());
    }

    @Test
    public void coinEmptyRefreshIntervalCheck() {
        deployContract(AccountBalanceNotifier.class);
        deployContract(SlackNotifier.class);

        sendCoinToBob(ALICE, AEUR);

        generateBlock();
        assertEquals(emptyList(), emulatedSlackMessages.getMessages());
        generateBlock();
        assertEquals(emptyList(), emulatedSlackMessages.getMessages());

        generateBlock();
        JO message = new JO();
        message.put("text", String.format(AccountBalanceNotifier.ALERT_MESSAGE_FORMAT, "ARDOR-XK4R-7VJU-6EQG-7R335", "AEUR", "coin", "1.0000", "7.0000", Nxt.getBlockchain().getHeight()));
        assertEquals(message.toJSONString(), emulatedSlackMessages.getMessages().get(0));
    }

    @Test
    public void coinEmptyDifferentAccount() {
        final Tester tester = DAVE;

        JO setupParams = new JO();
        setupParams.put("accountRs", tester.getRsAccount());

        deployContract(AccountBalanceNotifier.class, setupParams);
        deployContract(SlackNotifier.class);

        sendCoinToBob(tester, IGNIS);

        generateBlock();
        JO message = new JO();
        message.put("text", String.format(AccountBalanceNotifier.ALERT_MESSAGE_FORMAT, tester.getRsAccount(), "IGNIS", "coin", "1.00000000", "7.00000000", Nxt.getBlockchain().getHeight()));
        assertEquals(message.toJSONString(), emulatedSlackMessages.getMessages().get(0));
    }

    @Test
    public void assetEmpty() {
        final long assetId = createAndDistributeAsset(2);
        generateBlock();

        setRunnerConfig(createAssetAccountNotifierConfig(assetId));
        deployContract(AccountBalanceNotifier.class);
        deployContract(SlackNotifier.class);

        aliceSendAssetToBob(assetId);

        generateBlock();
        JO message = new JO();
        message.put("text", String.format(AccountBalanceNotifier.ALERT_MESSAGE_FORMAT, "ARDOR-XK4R-7VJU-6EQG-7R335", "AssetT", "asset", "1.00", "10.00", Nxt.getBlockchain().getHeight()));
        assertEquals(message.toJSONString(), emulatedSlackMessages.getMessages().get(0));
    }

    private JO createAssetAccountNotifierConfig(long assetId) {
        final JO result = AccessController.doPrivileged((PrivilegedAction<JO>) () -> {
            try (InputStream is = getClass().getResourceAsStream("./asset_config_ACBN.json")) {
                JSONObject o = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
                return new JO(o);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
        final JO assetLimit = new JO();
        assetLimit.put("accountRs", "ARDOR-XK4R-7VJU-6EQG-7R335");
        assetLimit.put("type", "asset");
        assetLimit.put("id", Long.toUnsignedString(assetId));
        assetLimit.put("minBalance", 10.0);
        assetLimit.put("refreshInterval", 1);

        JO jo = result.getJo("params").getJo("AccountBalanceNotifier");
        JA array = jo.getArray("limits");
        array.add(assetLimit);
        jo.put("limits", array);
        return result;
    }

    private void aliceSendAssetToBob(long assetId) {
        TransferAssetCall.create(IGNIS.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .recipient(BOB.getId())
                .asset(assetId)
                .quantityQNT(1_000 * 100_000_000L - 100L)
                .feeNQT(IGNIS.ONE_COIN)
                .build()
                .invokeNoError();
    }

    private void sendCoinToBob(Tester tester, ChildChain chain) {
        SendMoneyCall.create(chain.getId())
                .feeNQT(chain.ONE_COIN)
                .secretPhrase(tester.getSecretPhrase())
                .amountNQT(tester.getChainBalance(chain.getId()) - 2 * chain.ONE_COIN)
                .recipient(BOB.getId())
                .build()
                .invokeNoError();
    }

    private void setRunnerConfig(JO botConfig) {
        final byte[] bytes = botConfig.toJSONString().getBytes();
        setRunnerConfig(bytes);
    }
}