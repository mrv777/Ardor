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

package nxt.blockchain;

import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.ae.AssetDividendHome;
import nxt.ae.OrderHome;
import nxt.ae.TradeHome;
import nxt.aliases.AliasHome;
import nxt.aliases.AliasTransactionType;
import nxt.blockchain.chaincontrol.PermissionChecker;
import nxt.blockchain.chaincontrol.PermissionPolicy;
import nxt.blockchain.chaincontrol.PermissionPolicyType;
import nxt.blockchain.chaincontrol.PermissionReader;
import nxt.blockchain.chaincontrol.PermissionWriter;
import nxt.dgs.DigitalGoodsHome;
import nxt.dgs.DigitalGoodsTransactionType;
import nxt.http.APIEnum;
import nxt.http.APITag;
import nxt.ms.CurrencyFounderHome;
import nxt.ms.ExchangeHome;
import nxt.ms.ExchangeOfferHome;
import nxt.ms.ExchangeRequestHome;
import nxt.ms.MonetarySystemTransactionType;
import nxt.shuffling.ShufflingHome;
import nxt.shuffling.ShufflingParticipantHome;
import nxt.shuffling.ShufflingTransactionType;
import nxt.taggeddata.TaggedDataHome;
import nxt.taggeddata.TaggedDataTransactionType;
import nxt.util.Convert;
import nxt.voting.PhasingPollHome;
import nxt.voting.PhasingVoteHome;
import nxt.voting.PollHome;
import nxt.voting.VoteHome;
import nxt.voting.VotingTransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ChildChain extends Chain {

    private static final Map<String, ChildChain> childChains = new HashMap<>();
    private static final Map<Integer, ChildChain> childChainsById = new HashMap<>();

    private static final Collection<ChildChain> allChildChains = Collections.unmodifiableCollection(childChains.values());

    private static final Long[] defaultAdminAccounts = Constants.isAutomatedTest || Constants.isAutomatedTestChildChainPermissions
            ? new Long[]{Convert.parseAccountId("ARDOR-TZ39-8SMJ-U7G4-6M4UF"), Convert.parseAccountId("ARDOR-2G3B-KBMZ-6KX6-7P4LA")}
            : new Long[0];

    private static final PermissionPolicyType defaultNextPolicyType = Constants.isAutomatedTest || Constants.isAutomatedTestChildChainPermissions
            ? PermissionPolicyType.CHILD_CHAIN : null;

    private static final int defaultPolicyChangeHeight = Constants.isAutomatedTestChildChainPermissions
            ? 0 : Integer.MAX_VALUE;

    private static final Long[] aeurAdminAccounts = Constants.isAutomatedTest || Constants.isAutomatedTestChildChainPermissions
            ? defaultAdminAccounts : Constants.isTestnet ? new Long[]{Convert.parseAccountId("ARDOR-XNBG-5TB4-SC4R-B73ZW")} : new Long[0];

    public static final ChildChain IGNIS = new ChildChainBuilder(2, "IGNIS")
            .setTotalAmount(Constants.isTestnet ? 999_724_847_29793502L : 999_449_694_59860052L)
            .setShufflingDepositNQT(Constants.isTestnet ? 7 * 1_00000000 : 10 * 1_00000000)
            .setNextPermissionPolicy(defaultNextPolicyType, defaultPolicyChangeHeight)
            .setMasterAdminAccounts(defaultAdminAccounts)
            .build();

    public static final ChildChain AEUR = new ChildChainBuilder(3, "AEUR")
            .setDecimals(4)
            .setTotalAmount(Constants.isTestnet ? 999_724_845_5672L : 10_000_000_0000L)
            .setDisabledTransactionTypes(ShufflingTransactionType.SHUFFLING_CREATION)
            .setDisabledAPITags(APITag.SHUFFLING)
            .setNextPermissionPolicy(PermissionPolicyType.CHILD_CHAIN, Constants.isAutomatedTestChildChainPermissions ? 0 : Constants.PERMISSIONED_AEUR_BLOCK)
            .setMasterAdminAccounts(aeurAdminAccounts)
            .build();

    public static final ChildChain BITSWIFT = new ChildChainBuilder(4, "BITSWIFT")
            .setTotalAmount(Constants.isTestnet ? 3_884_634_74549710L : 3_884_634_74539339L)
            .setShufflingDepositNQT(10 * 1_00000000L)
            .setDisabledTransactionTypes(DigitalGoodsTransactionType.LISTING)
            .setDisabledAPITags(APITag.DGS)
            .setNextPermissionPolicy(defaultNextPolicyType, defaultPolicyChangeHeight)
            .setMasterAdminAccounts(defaultAdminAccounts)
            .build();

    public static final ChildChain MPG = new ChildChainBuilder(5, "MPG")
            .setTotalAmount(1_000_000_000_00000000L)
            .setDisabledTransactionTypes(
                    ShufflingTransactionType.SHUFFLING_CREATION, DigitalGoodsTransactionType.LISTING,
                    MonetarySystemTransactionType.CURRENCY_ISSUANCE, MonetarySystemTransactionType.CURRENCY_DELETION,
                    MonetarySystemTransactionType.CURRENCY_MINTING, MonetarySystemTransactionType.CURRENCY_TRANSFER,
                    MonetarySystemTransactionType.PUBLISH_EXCHANGE_OFFER, MonetarySystemTransactionType.EXCHANGE_BUY,
                    MonetarySystemTransactionType.EXCHANGE_SELL, MonetarySystemTransactionType.RESERVE_INCREASE,
                    MonetarySystemTransactionType.RESERVE_CLAIM, TaggedDataTransactionType.TAGGED_DATA_UPLOAD,
                    AliasTransactionType.ALIAS_ASSIGNMENT)
            .setDisabledAPITags(APITag.SHUFFLING, APITag.DGS, APITag.MS, APITag.DATA, APITag.ALIASES)
            .setIsEnabled(() -> Nxt.getBlockchain().getHeight() >= Constants.MPG_BLOCK)
            .build();

    public static final ChildChain GPS = new ChildChainBuilder(6, "GPS")
            .setTotalAmount(1_000_000_000_0000L)
            .setDecimals(4)
            .setDisabledTransactionTypes(
                    ShufflingTransactionType.SHUFFLING_CREATION, DigitalGoodsTransactionType.LISTING,
                    MonetarySystemTransactionType.CURRENCY_ISSUANCE, MonetarySystemTransactionType.CURRENCY_DELETION,
                    MonetarySystemTransactionType.CURRENCY_MINTING, MonetarySystemTransactionType.CURRENCY_TRANSFER,
                    MonetarySystemTransactionType.PUBLISH_EXCHANGE_OFFER, MonetarySystemTransactionType.EXCHANGE_BUY,
                    MonetarySystemTransactionType.EXCHANGE_SELL, MonetarySystemTransactionType.RESERVE_INCREASE,
                    MonetarySystemTransactionType.RESERVE_CLAIM, TaggedDataTransactionType.TAGGED_DATA_UPLOAD,
                    AliasTransactionType.ALIAS_ASSIGNMENT, VotingTransactionType.POLL_CREATION)
            .setDisabledAPITags(APITag.SHUFFLING, APITag.DGS, APITag.MS, APITag.DATA, APITag.ALIASES, APITag.VS)
            .setIsEnabled(() -> Nxt.getBlockchain().getHeight() >= Constants.GPS_BLOCK)
            .build();

    public static ChildChain getChildChain(String name) {
        return childChains.get(name);
    }

    public static ChildChain getChildChain(int id) {
        return childChainsById.get(id);
    }

    public static Collection<ChildChain> getAll() {
        return allChildChains;
    }

    public static void init() {
        ChildChainLoader.init();
    }

    public final long SHUFFLING_DEPOSIT_NQT;
    private final Supplier<PermissionPolicy> permissionPolicy;
    private final List<Long> masterAdminAccounts;
    private final AliasHome aliasHome;
    private final AssetDividendHome assetDividendHome;
    private final CurrencyFounderHome currencyFounderHome;
    private final DigitalGoodsHome digitalGoodsHome;
    private final ExchangeHome exchangeHome;
    private final ExchangeOfferHome exchangeOfferHome;
    private final ExchangeRequestHome exchangeRequestHome;
    private final OrderHome orderHome;
    private final PhasingPollHome phasingPollHome;
    private final PhasingVoteHome phasingVoteHome;
    private final PollHome pollHome;
    private final ShufflingHome shufflingHome;
    private final ShufflingParticipantHome shufflingParticipantHome;
    private final TaggedDataHome taggedDataHome;
    private final TradeHome tradeHome;
    private final VoteHome voteHome;
    private final Set<TransactionType> disabledTransactionTypes;
    private final BooleanSupplier isEnabled;

    private ChildChain(int id, String name, int decimals, long totalAmount, long shufflingDepositNQT, Set<TransactionType> disabledTransactionTypes,
                       EnumSet<APIEnum> disabledAPIs, EnumSet<APITag> disabledAPITags, BooleanSupplier isEnabled, PermissionPolicyType policyType, PermissionPolicyType nextPolicyType, int policyChangeHeight,
                       List<Long> masterAdminAccounts) {
        super(id, name, decimals, totalAmount, disabledAPIs, disabledAPITags);
        this.SHUFFLING_DEPOSIT_NQT = shufflingDepositNQT;
        final PermissionPolicy policy = policyType.create(this);
        if (nextPolicyType == null) {
            this.permissionPolicy = () -> policy;
        } else {
            final PermissionPolicy nextPolicy = nextPolicyType.create(this);
            this.permissionPolicy = () -> {
                if (Nxt.getBlockchain().getHeight() >= policyChangeHeight || Nxt.getBlockchain().getHeight() == 0 && nextPolicyType == PermissionPolicyType.CHILD_CHAIN) {
                    return nextPolicy;
                }
                return policy;
            };
        }
        this.aliasHome = AliasHome.forChain(this);
        this.assetDividendHome = AssetDividendHome.forChain(this);
        this.currencyFounderHome = CurrencyFounderHome.forChain(this);
        this.digitalGoodsHome = DigitalGoodsHome.forChain(this);
        this.exchangeHome = ExchangeHome.forChain(this);
        this.exchangeOfferHome = ExchangeOfferHome.forChain(this);
        this.exchangeRequestHome = ExchangeRequestHome.forChain(this);
        this.tradeHome = TradeHome.forChain(this);
        this.orderHome = OrderHome.forChain(this);
        this.phasingVoteHome = PhasingVoteHome.forChain(this);
        this.phasingPollHome = PhasingPollHome.forChain(this);
        this.pollHome = PollHome.forChain(this);
        this.shufflingHome = ShufflingHome.forChain(this);
        this.shufflingParticipantHome = ShufflingParticipantHome.forChain(this);
        this.taggedDataHome = TaggedDataHome.forChain(this);
        this.voteHome = VoteHome.forChain(this);
        this.disabledTransactionTypes = Collections.unmodifiableSet(disabledTransactionTypes);
        childChains.put(name, this);
        childChainsById.put(id, this);
        this.isEnabled = isEnabled;
        this.masterAdminAccounts = Collections.unmodifiableList(masterAdminAccounts);
    }

    public AliasHome getAliasHome() {
        return aliasHome;
    }

    public AssetDividendHome getAssetDividendHome() {
        return assetDividendHome;
    }

    public CurrencyFounderHome getCurrencyFounderHome() {
        return currencyFounderHome;
    }

    public DigitalGoodsHome getDigitalGoodsHome() {
        return digitalGoodsHome;
    }

    public ExchangeHome getExchangeHome() {
        return exchangeHome;
    }

    public ExchangeOfferHome getExchangeOfferHome() {
        return exchangeOfferHome;
    }

    public ExchangeRequestHome getExchangeRequestHome() {
        return exchangeRequestHome;
    }

    public OrderHome getOrderHome() {
        return orderHome;
    }

    public PhasingPollHome getPhasingPollHome() {
        return phasingPollHome;
    }

    public PhasingVoteHome getPhasingVoteHome() {
        return phasingVoteHome;
    }

    public PollHome getPollHome() {
        return pollHome;
    }

    public ShufflingHome getShufflingHome() {
        return shufflingHome;
    }

    public ShufflingParticipantHome getShufflingParticipantHome() {
        return shufflingParticipantHome;
    }

    public TaggedDataHome getTaggedDataHome() {
        return taggedDataHome;
    }

    public TradeHome getTradeHome() {
        return tradeHome;
    }

    public VoteHome getVoteHome() {
        return voteHome;
    }

    @Override
    public boolean isAllowed(TransactionType transactionType) {
        return transactionType.getType() >= 0 && !disabledTransactionTypes.contains(transactionType) && (this == IGNIS || !transactionType.isGlobal());
    }

    @Override
    public Set<TransactionType> getDisabledTransactionTypes() {
        return disabledTransactionTypes;
    }

    @Override
    public Set<APITag> getDisabledAPITags() {
        Set<APITag> permissionPolicyDisabledAPITags = getPermissionPolicy().getDisabledAPITags();
        if (permissionPolicyDisabledAPITags.isEmpty()) {
            return super.getDisabledAPITags();
        }
        Set<APITag> set = new HashSet<>(super.getDisabledAPITags());
        set.addAll(permissionPolicyDisabledAPITags);
        return set;
    }

    public boolean isEnabled() {
        return isEnabled.getAsBoolean();
    }

    @Override
    public ChildTransactionImpl.BuilderImpl newTransactionBuilder(byte[] senderPublicKey, long amount, long fee, short deadline, Attachment attachment) {
        return ChildTransactionImpl.newTransactionBuilder(this.getId(), (byte) 1, senderPublicKey, amount, fee, deadline, (Attachment.AbstractAttachment) attachment);
    }

    @Override
    ChildTransactionImpl.BuilderImpl newTransactionBuilder(byte version, byte[] senderPublicKey, long amount, long fee, short deadline,
                                                           List<Appendix.AbstractAppendix> appendages, JSONObject transactionData) {
        return ChildTransactionImpl.newTransactionBuilder(this.getId(), version, senderPublicKey, amount, fee, deadline,
                appendages, transactionData);
    }

    @Override
    ChildTransactionImpl.BuilderImpl newTransactionBuilder(byte version, byte[] senderPublicKey, long amount, long fee, short deadline,
                                                           List<Appendix.AbstractAppendix> appendages, ByteBuffer buffer) {
        return ChildTransactionImpl.newTransactionBuilder(this.getId(), version, senderPublicKey, amount, fee, deadline,
                appendages, buffer);
    }

    @Override
    ChildTransactionImpl.BuilderImpl newTransactionBuilder(byte version, long amount, long fee, short deadline,
                                                           List<Appendix.AbstractAppendix> appendages, ResultSet rs) {
        return ChildTransactionImpl.newTransactionBuilder(this.getId(), version, amount, fee, deadline, appendages, rs);
    }

    @Override
    UnconfirmedTransaction newUnconfirmedTransaction(ResultSet rs) throws SQLException, NxtException.NotValidException {
        return new UnconfirmedChildTransaction(rs);
    }

    public PermissionChecker getPermissionChecker() {
        return getPermissionPolicy().getPermissionChecker();
    }

    public PermissionWriter getPermissionWriter(long granterId) {
        return getPermissionPolicy().getPermissionWriter(granterId);
    }

    public PermissionReader getPermissionReader() {
        return getPermissionPolicy().getPermissionReader();
    }

    public PermissionPolicy getPermissionPolicy() {
        return permissionPolicy.get();
    }

    public List<Long> getMasterAdminAccounts() {
        return masterAdminAccounts;
    }

    private static class ChildChainBuilder {
        private final int id;
        private final String name;
        private long totalAmount;
        private long shufflingDepositNQT;
        private int decimals = 8;
        private Set<TransactionType> disabledTransactionTypes = Collections.emptySet();
        private EnumSet<APIEnum> disabledAPIs = EnumSet.noneOf(APIEnum.class);
        private EnumSet<APITag> disabledAPITags = EnumSet.noneOf(APITag.class);
        private BooleanSupplier isEnabled = () -> true;
        private PermissionPolicyType policyType = PermissionPolicyType.NONE;
        private PermissionPolicyType nextPolicyType = null;
        private int policyChangeHeight = 0;
        private List<Long> masterAdminAccounts = Collections.emptyList();

        private ChildChainBuilder(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private ChildChainBuilder setDecimals(int decimals) {
            this.decimals = decimals;
            return this;
        }

        private ChildChainBuilder setDisabledTransactionTypes(TransactionType... disabledTransactionTypes) {
            this.disabledTransactionTypes = linkedHashSet(disabledTransactionTypes);
            return this;
        }

        private ChildChainBuilder setDisabledAPIs(APIEnum... disabledAPIs) {
            this.disabledAPIs = enumSet(disabledAPIs);
            return this;
        }

        private ChildChainBuilder setDisabledAPITags(APITag... disabledAPITags) {
            this.disabledAPITags = enumSet(disabledAPITags);
            return this;
        }

        private ChildChainBuilder setTotalAmount(long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        private ChildChainBuilder setShufflingDepositNQT(long shufflingDepositNQT) {
            this.shufflingDepositNQT = shufflingDepositNQT;
            return this;
        }

        private ChildChainBuilder setIsEnabled(BooleanSupplier isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        private ChildChainBuilder setPermissionPolicy(PermissionPolicyType policyType) {
            this.policyType = policyType;
            return this;
        }

        private ChildChainBuilder setNextPermissionPolicy(PermissionPolicyType policyType, int policyChangeHeight) {
            this.nextPolicyType = policyType;
            this.policyChangeHeight = policyChangeHeight;
            return this;
        }

        private ChildChainBuilder setMasterAdminAccounts(Long... masterAdminAccounts) {
            this.masterAdminAccounts = Arrays.asList(masterAdminAccounts);
            return this;
        }

        private ChildChain build() {
            return new ChildChain(id, name, decimals, totalAmount, shufflingDepositNQT, disabledTransactionTypes,
                    disabledAPIs, disabledAPITags, isEnabled, policyType, nextPolicyType, policyChangeHeight, masterAdminAccounts);
        }

        @SafeVarargs
        private static <T extends Enum<T>> EnumSet<T> enumSet(T... elements) {
            return EnumSet.copyOf(Arrays.asList(elements));
        }

        private static Set<TransactionType> linkedHashSet(TransactionType... types) {
            return new LinkedHashSet<>(Arrays.asList(types));
        }
    }
}
