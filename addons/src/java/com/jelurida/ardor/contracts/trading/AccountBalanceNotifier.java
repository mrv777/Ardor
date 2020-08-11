/*
 * Copyright © 2020 Jelurida IP B.V.
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

package com.jelurida.ardor.contracts.trading;

import nxt.addons.AbstractContract;
import nxt.addons.AbstractContractContext;
import nxt.addons.BlockContext;
import nxt.addons.ChainWrapper;
import nxt.addons.Contract;
import nxt.addons.ContractAndSetupParameters;
import nxt.addons.ContractRunnerParameter;
import nxt.addons.DelegatedContext;
import nxt.addons.InitializationContext;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.http.callers.GetAccountAssetsCall;
import nxt.http.callers.GetAssetCall;
import nxt.http.callers.GetBalanceCall;
import nxt.http.responses.AssetEntityResponse;
import nxt.util.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A contact which monitors minimum balances for coins and assets and post a warning on a slack channel (configured via web-hook url)
 * in case any of the coin or asset balances goes below some threshold balance
 * (for instance the 10 ARDR necessary for the trading bot contract to operate).
 * <p>
 * Configuration:
 * "slackWebHookUrl" - points to web-hook of slack channel for notifications
 * "limits" - array of minimum balance limits
 * <p>
 * Here is contract runner config example:
 * <pre>
 "AccountBalanceNotifier": {
   "slackWebHookUrl": "",
   "limits": [{
     "accountRs": "ARDOR-XK4R-7VJU-6EQG-7R335",
     "type": "coin",
     "id": "ARDR",
     "minBalance": 10.0,
     "refreshInterval": 1
   },{
     "accountRs": "ARDOR-E93F-7E8Z-BHJ8-A65RG",
     "type": "asset",
     "id": "123",
     "minBalance": 7.0,
     "refreshInterval": 3
   }]
 }
 * </pre>
 */
public class AccountBalanceNotifier extends AbstractContract<Object, Object> {
    static final String ALERT_MESSAGE_FORMAT = "Alert: account %s has %s %s balance of %s, minimal balance is %s, block height is %d";

    private JO slackNotifierSetup;
    private final List<MinBalanceLimit> limits = new ArrayList<>();

    public interface Parameters {
        @ContractRunnerParameter
        String slackWebHookUrl();
    }

    @Override
    public void init(InitializationContext context) {
        Parameters params = context.getParams(Parameters.class);
        String slackWebHookUrl = params.slackWebHookUrl();
        if (slackWebHookUrl != null) {
            slackNotifierSetup = new JO();
            slackNotifierSetup.put("slackWebHookUrl", slackWebHookUrl);
        }

        JO contractRunnerConfigParams = context.getContractRunnerConfigParams(this.getClass().getSimpleName());
        JA limitsJson = contractRunnerConfigParams.getArray("limits");
        for (JO limit : limitsJson.objects()) {
            limits.add(MinBalanceLimit.create(context, limit));
        }
    }

    public static class MinBalanceLimit {
        private final String accountRs;
        private final String id;
        private final String friendlyName;
        private final long minBalanceQNT;
        private final int refreshInterval;
        private final HoldingType holdingType;
        private final int decimals;

        private MinBalanceLimit(String accountRs, String id, String friendlyName, long minBalance, int refreshInterval, HoldingType holdingType, int decimals) {
            this.accountRs = accountRs;
            this.id = id;
            this.friendlyName = friendlyName;
            this.refreshInterval = refreshInterval;
            this.holdingType = holdingType;
            this.minBalanceQNT = minBalance;
            this.decimals = decimals;
        }

        public static MinBalanceLimit create(AbstractContractContext context, JO limit) {
            String accountRs = limit.getString("accountRs");
            String id = limit.getString("id");
            int refreshInterval = limit.getInt("refreshInterval");
            Logger.logInfoMessage("loading limit for account %s holding id %s", accountRs, id);
            HoldingType holdingType = HoldingType.valueOf(limit.getString("type").toUpperCase());
            String friendlyName;
            int decimals;
            if (HoldingType.COIN == holdingType) {
                ChainWrapper chain = context.getChain(id.toUpperCase());
                if (chain == null) {
                    throw new IllegalArgumentException("Undefined coin");
                }
                friendlyName = chain.getName();
                decimals = chain.getDecimals();
            } else if (HoldingType.ASSET == holdingType) {
                long assetId = Long.parseUnsignedLong(id);
                AssetEntityResponse asset = AssetEntityResponse.create(GetAssetCall.create().asset(assetId).call());
                friendlyName = asset.getName();
                decimals = asset.getDecimals();
            } else {
                throw new IllegalArgumentException("Invalid holding type");
            }
            long minBalanceQNT = BigDecimal.valueOf(limit.getDouble("minBalance")).movePointRight(decimals).longValue();
            return new MinBalanceLimit(accountRs, id, friendlyName, minBalanceQNT, refreshInterval, holdingType, decimals);
        }

        BigDecimal fromQNT(long amount) {
            return new BigDecimal(amount).movePointLeft(decimals);
        }
    }

    @Override
    public JO processBlock(BlockContext context) {
        for (MinBalanceLimit limit : limits) {
            if (context.getHeight() % limit.refreshInterval != 0) {
                continue;
            }
            long balanceQNT = getBalanceQNT(context, limit);
            if (balanceQNT < limit.minBalanceQNT) {
                BigDecimal minBalance = limit.fromQNT(limit.minBalanceQNT);
                BigDecimal balance = limit.fromQNT(balanceQNT);
                String message = String.format(ALERT_MESSAGE_FORMAT, limit.accountRs, limit.friendlyName, limit.holdingType.name().toLowerCase(), balance.toPlainString(), minBalance.toPlainString(), context.getHeight());
                context.logInfoMessage(message);
                ContractAndSetupParameters contractAndParameters = context.loadContract("SlackNotifier");
                Contract<String, Integer> slackNotifier = contractAndParameters.getContract();
                int responseCode = slackNotifier.processInvocation(new DelegatedContext(context, "SlackNotifier", slackNotifierSetup), message);
                if (responseCode != 200) {
                    Logger.logInfoMessage("Slack notification failed");
                }
            }
        }
        return context.getResponse();
    }

    private long getBalanceQNT(AbstractContractContext context, MinBalanceLimit limit) {
        if (limit.holdingType == HoldingType.COIN) {
            JO response = GetBalanceCall.create(context.getChain(limit.id).getId()).account(limit.accountRs).call();
            return response.getLong("balanceNQT");
        } else if (limit.holdingType == HoldingType.ASSET) {
            JO response = GetAccountAssetsCall.create().account(limit.accountRs).asset(limit.id).call();
            return response.getLong("quantityQNT");
        }
        throw new IllegalStateException("Unsupported holding type: " + limit.holdingType);
    }

    public enum HoldingType {
        COIN, ASSET
    }
}
