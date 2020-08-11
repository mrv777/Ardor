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
package nxt.peer;

import nxt.Nxt;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.security.BlockchainPermission;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Filters the currently known {@link BundlerRate}s according to the provided {@link #minBalance}, {@link #minFeeLimit}
 * and {@link #whitelist}. Calculates the best NQT per FXT rate to be used when creating a child chain transaction. <br>
 *
 * The best rate is not always the cheapest one and depends on the {@link #priority} of the transaction
 */
public class FeeRateCalculator {
    public enum TransactionPriority {
        LOW,
        NORMAL,
        HIGH
    }

    /** Whitelisted accounts providing best bundler rate */
    private static final Set<Long> bestBundlerRateWhitelist;

    static {
        List<String> accountList = AccessController.doPrivileged((PrivilegedAction<List<String>>)() -> Nxt.getStringListProperty("nxt.bestBundlerRateWhitelist"));
        Set<Long> whitelistSet = new HashSet<>(accountList.size());
        accountList.forEach(account -> {
            try {
                long accountId = Convert.parseAccountId(account);
                whitelistSet.add(accountId);
                Logger.logInfoMessage("Added best bundler rate account " + Convert.rsAccount(accountId));
            } catch (Exception exc) {
                Logger.logDebugMessage("'" + account + "' is not a valid bundler account");
            }
        });
        bestBundlerRateWhitelist = Collections.unmodifiableSet(whitelistSet);
    }

    private static final Comparator<BundlerRate> BEST_RATE_COMPARATOR = (a, b) -> Long.signum(a.getRate() - b.getRate());

    /**
     * Minimum bundler effective account balance in FXT
     */
    private final long minBalance;

    /**
     * Minimum bundler remaining fee limit in FQT
     */
    private final long minFeeLimit;

    /**
     * Only rates from the whitelisted accounts will be considered. Defaults to the nxt.bestBundlerRateWhitelist config
     */
    private final Set<Long> whitelist;

    /**
     * Priority parameter used when calculating the fee - higher priority means more expensive transaction and higher
     * chance that the transaction gets bundled
     */
    private final TransactionPriority priority;

    public static class Builder {
        private long minBalance;

        private long minFeeLimit;

        private Set<Long> whitelist;

        private TransactionPriority priority;

        public Builder() {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new BlockchainPermission("getBundlerRates"));
            }
            this.minBalance = 0;
            this.minFeeLimit = 0;
            this.whitelist = bestBundlerRateWhitelist;
            this.priority = TransactionPriority.NORMAL;
        }

        /**
         * @param minBalance The set min balance
         * @return the builder
         * @see FeeRateCalculator#minBalance
         */
        public Builder setMinBalance(long minBalance) {
            this.minBalance = minBalance;
            return this;
        }

        /**
         * @param minFeeLimit The set value
         * @return the builder
         * @see FeeRateCalculator#minFeeLimit
         */
        public Builder setMinFeeLimit(long minFeeLimit) {
            this.minFeeLimit = minFeeLimit;
            return this;
        }

        /**
         * @param whitelist The set value
         * @return the builder
         * @see FeeRateCalculator#whitelist
         */
        public Builder setWhitelist(Set<Long> whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        /**
         * @param priority The set value
         * @return the builder
         * @see FeeRateCalculator#priority
         */
        public Builder setPriority(TransactionPriority priority) {
            this.priority = priority;
            return this;
        }

        public FeeRateCalculator build() {
            return new FeeRateCalculator(minBalance, minFeeLimit, whitelist, priority);
        }
    }

    public static FeeRateCalculator.Builder create() {
        return new FeeRateCalculator.Builder();
    }

    private FeeRateCalculator(long minBalance, long minFeeLimit, Set<Long> whitelist, TransactionPriority priority) {
        this.minBalance = minBalance;
        this.minFeeLimit = minFeeLimit;
        this.whitelist = whitelist;
        this.priority = priority;
    }

    public long getMinBalance() {
        return minBalance;
    }

    public long getMinFeeLimit() {
        return minFeeLimit;
    }

    public Set<Long> getWhitelist() {
        return whitelist;
    }

    public TransactionPriority getPriority() {
        return priority;
    }

    /**
     * @return The best bundler rate for each child chain
     */
    public List<BundlerRate> getBestRates() {
        Map<ChildChain, TreeSet<BundlerRate>> rateMap = new HashMap<>();
        Peers.forEachBundlerRate(rate -> {
            if (filterBundlerRate(rate)) {
                rateMap.computeIfAbsent(rate.getChain(), k -> new TreeSet<>(BEST_RATE_COMPARATOR)).add(rate);
            }
        });
        List<BundlerRate> bestRates = new ArrayList<>();
        rateMap.forEach((key, chainRates) -> findBestRate(chainRates).ifPresent(bestRates::add));
        return bestRates;
    }

    /**
     * @param childChain The child chain we are interested in
     *
     * @return The best NQT per FXT rate for a child chain
     */
    public long getBestRate(Chain childChain) {
        TreeSet<BundlerRate> chainRates = new TreeSet<>(BEST_RATE_COMPARATOR);
        Peers.forEachBundlerRate(rate -> {
            if (rate.getChain() == childChain && filterBundlerRate(rate)) {
                chainRates.add(rate);
            }
        });
        return findBestRate(chainRates).map(BundlerRate::getRate).orElse(-1L);
    }

    private boolean filterBundlerRate(BundlerRate rate) {
        if (whitelist != null && !whitelist.isEmpty() && !whitelist.contains(rate.getAccountId())) {
            return false;
        }
        return rate.getBalance() >= minBalance && rate.getFeeLimit() >= minFeeLimit
                && AccessController.doPrivileged(
                        (PrivilegedAction<Long>) () ->
                                FxtChain.FXT.getBalanceHome().getBalance(rate.getAccountId()).getUnconfirmedBalance()) >= minFeeLimit;
    }

    private Optional<BundlerRate> findBestRate(TreeSet<BundlerRate> sortedChainRates) {
        int ratesToSkip = priority.ordinal();
        Optional<BundlerRate> result = Optional.empty();
        for (BundlerRate rate : sortedChainRates) {
            result = Optional.of(rate);
            if (ratesToSkip == 0) {
                break;
            }
            ratesToSkip--;
        }
        return result;
    }
}
