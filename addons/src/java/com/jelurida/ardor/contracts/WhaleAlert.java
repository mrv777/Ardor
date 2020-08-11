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

package com.jelurida.ardor.contracts;

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
import nxt.addons.RequestContext;
import nxt.http.callers.GetAccountCall;
import nxt.http.callers.GetAllTradesCall;
import nxt.http.callers.GetAssetCall;
import nxt.http.callers.GetBlockCall;
import nxt.http.callers.GetCoinExchangeTradesCall;
import nxt.http.callers.GetExecutedTransactionsCall;
import nxt.http.responses.AssetEntityResponse;
import nxt.http.responses.AssetExchangeTradeResponse;
import nxt.http.responses.BlockResponse;
import nxt.http.responses.CoinExchangeTradeResponse;
import nxt.http.responses.TransactionResponse;
import nxt.util.Convert;
import nxt.util.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A contact which monitors new transactions and asset trades and report alerts
 * when high value transaction or trade is observed.
 * <p>
 * Configuration:
 * "slackWebHookUrl" - points to web-hook of slack channel for notifications
 * "thresholds" - array of thresholds defining holding and threshold to monitor
 */
public class WhaleAlert extends AbstractContract<Object, Object> {
    static final String ALERT_MESSAGE_FORMAT = "%s Whale Alert: %.2f %s %ss %s at height %d %s";

    private static JO slackNotifierSetup;
    private final List<AlertThreshold> thresholds = new ArrayList<>();

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
        JA thresholdJson = contractRunnerConfigParams.getArray("thresholds");
        for (JO threshold : thresholdJson.objects()) {
            thresholds.add(AlertThreshold.create(context, threshold));
        }
    }

    public static class AlertThreshold {
        private final long id;
        private final String friendlyName;
        private final long thresholdAmountQNT;
        private final HoldingType holdingType;
        private final int decimals;

        private AlertThreshold(long id, String friendlyName, long thresholdAmountQNT, HoldingType holdingType, int decimals) {
            this.id = id;
            this.friendlyName = friendlyName;
            this.holdingType = holdingType;
            this.thresholdAmountQNT = thresholdAmountQNT;
            this.decimals = decimals;
        }

        public static AlertThreshold create(AbstractContractContext context, JO limit) {
            String idStr = limit.getString("id");
            HoldingType holdingType = HoldingType.valueOf(limit.getString("type").toUpperCase());
            long id;
            String friendlyName;
            int decimals;
            if (holdingType == HoldingType.COIN) {
                ChainWrapper chain = context.getChain(idStr.toUpperCase());
                if (chain == null) {
                    throw new IllegalArgumentException("Undefined coin " + idStr);
                }
                id = chain.getId();
                friendlyName = chain.getName();
                decimals = chain.getDecimals();
            } else if (holdingType == HoldingType.ASSET) {
                JO getAssetResponse = GetAssetCall.create().asset(idStr).call();
                if (getAssetResponse.isExist("errorCode")) {
                    throw new IllegalArgumentException("Asset " + idStr + " does not exist");
                }
                AssetEntityResponse asset = AssetEntityResponse.create(getAssetResponse);
                id = asset.getAsset();
                friendlyName = asset.getName();
                decimals = asset.getDecimals();
            } else {
                throw new IllegalArgumentException("Invalid holding type");
            }
            long thresholdBalanceQNT = BigDecimal.valueOf(limit.getDouble("thresholdBalance")).movePointRight(decimals).longValue();
            return new AlertThreshold(id, friendlyName, thresholdBalanceQNT, holdingType, decimals);
        }

        BigDecimal fromQNT(long amount) {
            return new BigDecimal(amount).movePointLeft(decimals);
        }

        private String getFromToMessage(TransactionResponse transaction) {
            JO senderAccount = GetAccountCall.create().account(transaction.getSenderId()).call();
            String senderName = senderAccount.getString("accountRS");
            if (senderAccount.isExist("name")) {
                senderName += " (" + senderAccount.getString("name") + ")";
            }
            JO recipientAccount = GetAccountCall.create().account(transaction.getRecipientId()).call();
            String recipientName = recipientAccount.getString("accountRS");
            if (recipientAccount.isExist("name")) {
                recipientName += " (" + recipientAccount.getString("name") + ")";
            }
            return String.format(" from %s to %s", senderName, recipientName);
        }

        void inspectAmount(AbstractContractContext context, TransactionResponse transaction, int height) {
            if (id == transaction.getChainId()) {
                if (thresholdAmountQNT <= transaction.getAmount()) {
                    raiseAlert(context, "paid", transaction.getAmount(), height, getFromToMessage(transaction));
                }
            }
        }

        void inspectExchange(AbstractContractContext context, CoinExchangeTradeResponse exchange, int height) {
            if (id == exchange.getChainId()) {
                if (thresholdAmountQNT <= exchange.getQuantityQNT()) {
                    raiseAlert(context, "exchanged", exchange.getQuantityQNT(), height, "");
                }
            } else if (id == exchange.getExchangeChainId()) {
                int otherChainDecimals = context.getChain(exchange.getChainId()).getDecimals();
                BigDecimal multiply = BigDecimal.valueOf(exchange.getQuantityQNT()).multiply(BigDecimal.valueOf(exchange.getPriceNQTPerCoin()));
                long exchangeQuantityQNT = multiply.movePointLeft(otherChainDecimals).longValue();
                if (thresholdAmountQNT <= exchangeQuantityQNT) {
                    raiseAlert(context, "exchanged", exchangeQuantityQNT, height, "");
                }
            }
        }

        void inspectAssetTransferAmount(AbstractContractContext context, TransactionResponse transaction, int height) {
            JO attachmentJson = transaction.getAttachmentJson();
            long amount = attachmentJson.getLong("quantityQNT");
            long assetId = attachmentJson.getEntityId("asset");
            if (id == assetId) {
                if (thresholdAmountQNT <= amount) {
                    raiseAlert(context, "transferred", amount, height, getFromToMessage(transaction));
                }
            }
        }

        void inspectTrade(AbstractContractContext context, AssetExchangeTradeResponse trade, int height) {
            if (id == trade.getAsset()) {
                if (thresholdAmountQNT <= trade.getQuantityQNT()) {
                    raiseAlert(context, "traded", trade.getQuantityQNT(), height, "");
                }
            }
        }

        private void raiseAlert(AbstractContractContext context, String operation, long amountQNT, int height, String fromToMessage) {
            BigDecimal amount = fromQNT(amountQNT);
            String message = String.format(ALERT_MESSAGE_FORMAT, context.getNetworkType(), amount.doubleValue(), friendlyName, holdingType.name().toLowerCase(), operation, height, fromToMessage);
            context.logInfoMessage(message);
            if (context.canSetResponse()) {
                JO response = context.getResponse();
                if (response == null) {
                    response = new JO();
                }
                JA messages;
                if (response.isExist("messages")) {
                    messages = response.getArray("messages");
                } else {
                    messages = new JA();
                }
                JO messageJson = new JO();
                messageJson.put("message", message);
                messages.add(messageJson);
                response.put("messages", messages);
                context.setResponse(response);
            }
            ContractAndSetupParameters contractAndParameters = context.loadContract("SlackNotifier");
            Contract<String, Integer> slackNotifier = contractAndParameters.getContract();
            int responseCode = slackNotifier.processInvocation(new DelegatedContext(context, "SlackNotifier", slackNotifierSetup), message);
            if (responseCode != 200) {
                Logger.logInfoMessage("Slack notification failed");
            }
        }
    }

    @Override
    public JO processBlock(BlockContext context) {
        BlockResponse block = context.getBlock();
        return checkThresholds(context, block);
    }

    private JO checkThresholds(AbstractContractContext context, BlockResponse block) {
        int height = block.getHeight();
        int chainId = 1;
        List<AlertThreshold> coinThresholds = thresholds.stream().filter(t -> t.holdingType == HoldingType.COIN).collect(Collectors.toList());
        List<AlertThreshold> assetThresholds = thresholds.stream().filter(t -> t.holdingType == HoldingType.ASSET).collect(Collectors.toList());
        while ((context.getChain(chainId)) != null) {
            int type = chainId == 1 ? -2 : 0;
            JO call = GetExecutedTransactionsCall.create(chainId).type(type).subtype(0).height(block.getHeight()).call();
            List<TransactionResponse> transactions = call.getJoList("transactions").stream().map(TransactionResponse::create).collect(Collectors.toList());
            for (TransactionResponse transaction : transactions) {
                for (AlertThreshold threshold : coinThresholds) {
                    threshold.inspectAmount(context, transaction, height);
                }
            }
            JO coinExchanges = GetCoinExchangeTradesCall.create().call();
            List<CoinExchangeTradeResponse> exchanges = coinExchanges.getJoList("trades").stream().map(CoinExchangeTradeResponse::create).collect(Collectors.toList());
            for (CoinExchangeTradeResponse exchange : exchanges) {
                if (exchange.getTimestamp() < block.getTimestamp()) {
                    // response is sorted by height descending
                    break;
                }
                for (AlertThreshold threshold : coinThresholds) {
                    threshold.inspectExchange(context, exchange, height);
                }
            }
            if (chainId > 1) {
                call = GetExecutedTransactionsCall.create(chainId).type(2).subtype(1).height(block.getHeight()).call();
                transactions = call.getJoList("transactions").stream().map(TransactionResponse::create).collect(Collectors.toList());
                for (TransactionResponse transaction : transactions) {
                    for (AlertThreshold threshold : assetThresholds) {
                        threshold.inspectAssetTransferAmount(context, transaction, height);
                    }
                }
            }
            JO tradesJson = GetAllTradesCall.create(chainId).timestamp(block.getTimestamp()).call();
            List<AssetExchangeTradeResponse> trades = tradesJson.getJoList("trades").stream().map(AssetExchangeTradeResponse::create).collect(Collectors.toList());
            for (AssetExchangeTradeResponse trade : trades) {
                for (AlertThreshold threshold : assetThresholds) {
                    threshold.inspectTrade(context, trade, height);
                }
            }
            chainId++;
        }
        return context.getResponse();
    }

    @Override
    public JO processRequest(RequestContext context) {
        String heightStr = Convert.emptyToNull(context.getParameter("height"));
        JO response = new JO();
        if (heightStr == null) {
            response.put("errorCode", 10001);
            response.put("errorDescription", "missing height");
            return response;
        }
        int height = Integer.parseInt(heightStr);
        BlockResponse block = BlockResponse.create(GetBlockCall.create().height(height).call());
        String thresholdHoldingType = Convert.emptyToNull(context.getParameter("type"));
        if (thresholdHoldingType != null) {
            String id = context.getParameter("id");
            String thresholdBalance = context.getParameter("thresholdBalance");
            JO limit = new JO.Builder().put("type", thresholdHoldingType).put("id", id).put("thresholdBalance", thresholdBalance).build();
            AlertThreshold alertThreshold = AlertThreshold.create(context, limit);
            thresholds.add(alertThreshold);
        }
        checkThresholds(context, block);
        return context.getResponse();
    }

    public enum HoldingType {
        COIN, ASSET
    }
}
