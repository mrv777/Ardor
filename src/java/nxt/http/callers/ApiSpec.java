// Auto generated code, do not modify
package nxt.http.callers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ApiSpec {
    getLastExchanges(true, null, "chain", "currencies", "currencies", "currencies", "includeCurrencyInfo", "requireBlock", "requireLastBlock"),

    addAccountPermission(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "permission", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    startFundingMonitor(true, null, "chain", "holdingType", "holding", "property", "amount", "threshold", "interval", "secretPhrase", "feeRateNQTPerFXT", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getExpectedAskOrders(true, null, "chain", "asset", "sortByPrice", "requireBlock", "requireLastBlock"),

    getChainPermissions(true, null, "chain", "permission", "granter", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountPublicKey(false, null, "account", "requireBlock", "requireLastBlock"),

    detectMimeType(false, new String[] {"file"}, "data", "filename", "isText"),

    getAccountPermissions(true, null, "chain", "account", "requireBlock", "requireLastBlock"),

    getBlocks(false, null, "firstIndex", "lastIndex", "timestamp", "includeTransactions", "includeExecutedPhased", "adminPassword", "requireBlock", "requireLastBlock"),

    getAssetsByIssuer(false, null, "account", "account", "account", "firstIndex", "lastIndex", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    getExchangesByOffer(true, null, "chain", "offer", "includeCurrencyInfo", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAllOpenBidOrders(true, null, "chain", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    dgsPurchase(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "goods", "priceNQT", "quantity", "deliveryDeadlineTimestamp", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAccountBlockCount(false, null, "account", "requireBlock", "requireLastBlock"),

    deleteAlias(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "alias", "aliasName", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    decodeFileToken(false, new String[] {"file"}, "token"),

    getPlugins(false, null, ""),

    addBundlingRule(true, null, "chain", "secretPhrase", "minRateNQTPerFXT", "totalFeesLimitFQT", "overpayFQTPerFXT", "feeCalculatorName", "filter", "filter", "filter", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getPhasingAssetControl(false, null, "asset", "requireBlock", "requireLastBlock"),

    getDataTagsLike(true, null, "chain", "tagPrefix", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getFundingMonitor(true, null, "chain", "holdingType", "holding", "property", "secretPhrase", "includeMonitoredAccounts", "includeHoldingInfo", "account", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getPolls(true, null, "chain", "account", "firstIndex", "lastIndex", "timestamp", "includeFinished", "finishedOnly", "adminPassword", "requireBlock", "requireLastBlock"),

    downloadTaggedData(true, null, "chain", "transactionFullHash", "retrieve", "requireBlock", "requireLastBlock"),

    getDataTags(true, null, "chain", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    processVoucher(false, new String[] {"voucher"}, "secretPhrase", "validate", "broadcast", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getPollVote(true, null, "chain", "poll", "account", "includeWeights", "requireBlock", "requireLastBlock"),

    getAPIProxyReports(false, null, "adminPassword"),

    addPeer(false, null, "peer", "adminPassword"),

    getSharedKey(false, null, "account", "secretPhrase", "nonce", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    decodeToken(false, null, "website", "token"),

    popOff(false, null, "numBlocks", "height", "keepTransactions", "adminPassword"),

    getAccountPhasedTransactions(true, null, "chain", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAvailableToBuy(true, null, "chain", "currency", "unitsQNT", "requireBlock", "requireLastBlock"),

    getExecutedTransactions(true, null, "chain", "height", "numberOfConfirmations", "type", "subtype", "sender", "recipient", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getNextBlockGenerators(false, null, "limit"),

    getExpectedAssetDeletes(true, null, "chain", "asset", "account", "includeAssetInfo", "requireBlock", "requireLastBlock"),

    getCoinExchangeOrderIds(true, null, "chain", "exchange", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    setContractReference(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "contractName", "contractParams", "contract", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    startForging(false, null, "secretPhrase", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    triggerContractByVoucher(false, new String[] {"voucher"}, "contractName", "requireBlock", "requireLastBlock"),

    getAssetAccounts(false, null, "asset", "height", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getCurrencyFounders(true, null, "chain", "currency", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    currencyBuy(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "rateNQTPerUnit", "unitsQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    decodeQRCode(false, null, "qrCodeBase64"),

    getAllExchanges(true, null, "chain", "timestamp", "firstIndex", "lastIndex", "includeCurrencyInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getCurrencyTransfers(false, null, "currency", "account", "firstIndex", "lastIndex", "timestamp", "includeCurrencyInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getExpectedOrderCancellations(true, null, "chain", "requireBlock", "requireLastBlock"),

    eventRegister(false, null, "event", "event", "event", "token", "add", "remove"),

    scan(false, null, "numBlocks", "height", "validate", "adminPassword"),

    getAllBundlerRates(false, null, "minBundlerBalanceFXT"),

    hexConvert(false, null, "string"),

    getPhasingOnlyControl(false, null, "account", "requireBlock", "requireLastBlock"),

    getDGSTagCount(true, null, "chain", "inStockOnly", "requireBlock", "requireLastBlock"),

    taxReport(false, null, "fromHeight", "toHeight", "account", "account", "fromDate", "toDate", "timeZone", "dateFormat", "delimiter", "unknownEventPolicy", "adminPassword"),

    getOffer(true, null, "chain", "offer", "requireBlock", "requireLastBlock"),

    encodeQRCode(false, null, "qrCodeData", "width", "height"),

    getChannelTaggedData(true, null, "chain", "channel", "account", "firstIndex", "lastIndex", "includeData", "adminPassword", "requireBlock", "requireLastBlock"),

    getAvailableToSell(true, null, "chain", "currency", "unitsQNT", "requireBlock", "requireLastBlock"),

    cancelBidOrder(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "order", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    shufflingCancel(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "shufflingFullHash", "cancellingAccount", "shufflingStateHash", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAssetProperties(false, null, "asset", "setter", "property", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    parsePhasingParams(false, null, "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingMinBalanceModel", "phasingHolding", "chain", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingExpression", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue"),

    getAccount(false, null, "account", "includeLessors", "includeAssets", "includeCurrencies", "includeEffectiveBalance", "requireBlock", "requireLastBlock"),

    blacklistAPIProxyPeer(false, null, "peer", "adminPassword"),

    getPeer(false, null, "peer"),

    getAccountCurrentAskOrderIds(true, null, "chain", "account", "asset", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getUnconfirmedTransactionIds(true, null, "chain", "account", "account", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountShufflings(true, null, "chain", "account", "includeFinished", "includeHoldingInfo", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getExpectedSellOffers(true, null, "chain", "currency", "account", "sortByRate", "requireBlock", "requireLastBlock"),

    getBundlingOptions(false, null, ""),

    dgsPriceChange(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "goods", "priceNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAliasesLike(true, null, "chain", "aliasPrefix", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    dgsListing(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "name", "description", "tags", "quantity", "priceNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getBidOrder(true, null, "chain", "order", "requireBlock", "requireLastBlock"),

    sendMessage(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAllBroadcastedTransactions(false, null, "adminPassword", "requireBlock", "requireLastBlock"),

    deriveAccountFromSeed(false, null, "mnemonic", "passphrase", "bip32Path"),

    placeBidOrder(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "quantityQNT", "priceNQTPerShare", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAccountBlocks(false, null, "account", "timestamp", "firstIndex", "lastIndex", "includeTransactions", "adminPassword", "requireBlock", "requireLastBlock"),

    getShuffling(true, null, "chain", "shufflingFullHash", "includeHoldingInfo", "requireBlock", "requireLastBlock"),

    setAPIProxyPeer(false, null, "peer", "adminPassword"),

    getAccountCurrencies(false, null, "account", "currency", "height", "includeCurrencyInfo", "requireBlock", "requireLastBlock"),

    getExpectedTransactions(true, null, "chain", "account", "account", "account", "requireBlock", "requireLastBlock"),

    getAccountCurrentBidOrderIds(true, null, "chain", "account", "asset", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAllPhasingOnlyControls(false, null, "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getExpectedCoinExchangeOrderCancellations(true, null, "chain", "requireBlock", "requireLastBlock"),

    dgsRefund(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "purchase", "refundNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAssetIds(false, null, "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getTaggedData(true, null, "chain", "transactionFullHash", "includeData", "retrieve", "requireBlock", "requireLastBlock"),

    stopStandbyShuffler(true, null, "chain", "secretPhrase", "holdingType", "holding", "account", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    searchAccounts(false, null, "query", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountLedger(false, null, "account", "firstIndex", "lastIndex", "eventType", "event", "holdingType", "holding", "includeTransactions", "includeHoldingInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountAssets(false, null, "account", "asset", "height", "includeAssetInfo", "requireBlock", "requireLastBlock"),

    deleteAccountProperty(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "property", "setter", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getBlockchainTransactions(true, null, "chain", "account", "timestamp", "type", "subtype", "firstIndex", "lastIndex", "numberOfConfirmations", "withMessage", "phasedOnly", "nonPhasedOnly", "includeExpiredPrunable", "includePhasingResult", "executedOnly", "adminPassword", "requireBlock", "requireLastBlock"),

    sendMoney(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "amountNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getMyInfo(false, null, ""),

    getAccountTaggedData(true, null, "chain", "account", "firstIndex", "lastIndex", "includeData", "adminPassword", "requireBlock", "requireLastBlock"),

    getAllTrades(true, null, "chain", "timestamp", "firstIndex", "lastIndex", "includeAssetInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    uploadContractRunnerConfiguration(false, new String[] {"config"}, "adminPassword", "requireBlock", "requireLastBlock"),

    splitSecret(false, null, "secret", "privateKey", "totalPieces", "minimumPieces", "primeFieldSize"),

    getStackTraces(false, null, "depth", "adminPassword"),

    rsConvert(false, null, "account"),

    searchTaggedData(true, null, "chain", "query", "tag", "channel", "account", "firstIndex", "lastIndex", "includeData", "adminPassword", "requireBlock", "requireLastBlock"),

    getAllTaggedData(true, null, "chain", "firstIndex", "lastIndex", "includeData", "adminPassword", "requireBlock", "requireLastBlock"),

    calculateFee(false, null, "transactionJSON", "transactionBytes", "prunableAttachmentJSON", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT"),

    saveContractRunnerEncrypted(false, null, "path", "dataAlreadyEncrypted", "encryptionPassword", "contractRunner", "adminPassword"),

    getDGSPendingPurchases(true, null, "chain", "seller", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getECBlock(false, null, "timestamp", "requireBlock", "requireLastBlock"),

    getCoinExchangeTrades(true, null, "chain", "exchange", "account", "orderFullHash", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    generateFileToken(false, new String[] {"file"}, "secretPhrase", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    searchDGSGoods(true, null, "chain", "query", "tag", "seller", "firstIndex", "lastIndex", "inStockOnly", "hideDelisted", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountPhasedTransactionCount(true, null, "chain", "account", "requireBlock", "requireLastBlock"),

    getCurrencyAccounts(false, null, "currency", "height", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    shufflingCreate(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "holding", "holdingType", "amount", "participantCount", "registrationPeriod", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    stopBundler(true, null, "chain", "account", "secretPhrase", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getAlias(true, null, "chain", "alias", "aliasName", "requireBlock", "requireLastBlock"),

    canDeleteCurrency(false, null, "account", "currency", "requireBlock", "requireLastBlock"),

    startContractRunnerEncrypted(false, null, "path", "encryptionPassword"),

    managePeersNetworking(false, null, "operation", "adminPassword"),

    getPhasingPollVote(true, null, "chain", "transactionFullHash", "account", "requireBlock", "requireLastBlock"),

    stopFundingMonitor(true, null, "chain", "holdingType", "holding", "property", "secretPhrase", "account", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getTime(false, null, ""),

    buyAlias(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "alias", "aliasName", "amountNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    searchPolls(true, null, "chain", "query", "firstIndex", "lastIndex", "includeFinished", "adminPassword", "requireBlock", "requireLastBlock"),

    simulateCoinExchange(true, null, "chain", "exchange", "quantityQNT", "priceNQTPerCoin"),

    eventWait(false, null, "token", "timeout"),

    castVote(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "poll", "vote00", "vote01", "vote02", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getMintingTarget(false, null, "currency", "account", "unitsQNT", "requireBlock", "requireLastBlock"),

    generateToken(false, null, "website", "secretPhrase", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    longConvert(false, null, "id"),

    getBlockId(false, null, "height", "requireBlock", "requireLastBlock"),

    getLastTrades(true, null, "chain", "assets", "assets", "assets", "includeAssetInfo", "requireBlock", "requireLastBlock"),

    getExpectedBidOrders(true, null, "chain", "asset", "sortByPrice", "requireBlock", "requireLastBlock"),

    setPhasingAssetControl(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "controlVotingModel", "controlQuorum", "controlMinBalance", "controlMinBalanceModel", "controlHolding", "controlWhitelisted", "controlWhitelisted", "controlWhitelisted", "controlSenderPropertySetter", "controlSenderPropertyName", "controlSenderPropertyValue", "controlRecipientPropertySetter", "controlRecipientPropertyName", "controlRecipientPropertyValue", "controlExpression", "controlParams", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    cancelCoinExchange(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "order", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getBidOrderIds(true, null, "chain", "asset", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getBlockchainStatus(false, null, ""),

    getConstants(false, null, ""),

    getLedgerMasterPublicKey(false, null, "bip32Path", "adminPassword"),

    setConfiguration(false, null, "propertiesJSON", "shutdown", "adminPassword"),

    getTransaction(true, null, "chain", "fullHash", "includePhasingResult", "requireBlock", "requireLastBlock"),

    getBlock(false, null, "block", "height", "timestamp", "includeTransactions", "includeExecutedPhased", "requireBlock", "requireLastBlock"),

    verifyTaggedData(true, new String[] {"file"}, "chain", "transactionFullHash", "name", "description", "tags", "type", "channel", "isText", "filename", "data", "requireBlock", "requireLastBlock"),

    getExchangesByExchangeRequest(true, null, "chain", "transaction", "includeCurrencyInfo", "requireBlock", "requireLastBlock"),

    getPrunableMessage(true, null, "chain", "transactionFullHash", "secretPhrase", "sharedKey", "retrieve", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    dividendPayment(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "holding", "holdingType", "asset", "height", "amountNQTPerShare", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    broadcastTransaction(false, null, "transactionJSON", "transactionBytes", "prunableAttachmentJSON"),

    currencySell(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "rateNQTPerUnit", "unitsQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    blacklistPeer(false, null, "peer", "adminPassword"),

    dgsDelivery(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "purchase", "discountNQT", "goodsToEncrypt", "goodsIsText", "goodsData", "goodsNonce", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    setAccountProperty(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "property", "value", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    startStandbyShuffler(true, null, "chain", "secretPhrase", "holdingType", "holding", "minAmount", "maxAmount", "minParticipants", "feeRateNQTPerFXT", "recipientPublicKeys", "serializedMasterPublicKey", "startFromChildIndex", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getShufflers(false, null, "account", "shufflingFullHash", "secretPhrase", "adminPassword", "includeParticipantState", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getDGSGoodsPurchaseCount(true, null, "chain", "goods", "withPublicFeedbacksOnly", "completed", "requireBlock", "requireLastBlock"),

    sendTransaction(false, null, "transactionJSON", "transactionBytes", "prunableAttachmentJSON", "adminPassword"),

    getAssignedShufflings(true, null, "chain", "account", "includeHoldingInfo", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getGuaranteedBalance(false, null, "account", "numberOfConfirmations", "requireBlock", "requireLastBlock"),

    fullHashToId(false, null, "fullHash"),

    getExpectedBuyOffers(true, null, "chain", "currency", "account", "sortByRate", "requireBlock", "requireLastBlock"),

    getAskOrders(true, null, "chain", "asset", "firstIndex", "lastIndex", "showExpectedCancellations", "adminPassword", "requireBlock", "requireLastBlock"),

    stopForging(false, null, "secretPhrase", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getAccountExchangeRequests(true, null, "chain", "account", "currency", "includeCurrencyInfo", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    downloadPrunableMessage(true, null, "chain", "transactionFullHash", "secretPhrase", "sharedKey", "retrieve", "save", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getAsset(false, null, "asset", "includeCounts", "requireBlock", "requireLastBlock"),

    clearUnconfirmedTransactions(false, null, "adminPassword"),

    getBundlers(true, null, "chain", "account", "secretPhrase", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getHoldingShufflings(true, null, "chain", "holding", "stage", "includeFinished", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAssetDividends(true, null, "chain", "asset", "firstIndex", "lastIndex", "timestamp", "includeHoldingInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getEffectiveBalance(false, null, "account", "height", "requireBlock", "requireLastBlock"),

    getAssetPhasedTransactions(true, null, "chain", "asset", "account", "withoutWhitelist", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    deriveAccountFromMasterPublicKey(false, null, "serializedMasterPublicKey", "childIndex"),

    removeAccountPermission(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "permission", "height", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAccountCurrentBidOrders(true, null, "chain", "account", "asset", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    dgsQuantityChange(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "goods", "deltaQuantity", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getExpectedCurrencyTransfers(true, null, "chain", "currency", "account", "includeCurrencyInfo", "requireBlock", "requireLastBlock"),

    cancelAskOrder(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "order", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    evaluateExpression(false, null, "expression", "checkOptimality", "evaluate", "vars", "vars", "vars", "values", "values", "values"),

    searchAssets(false, null, "query", "firstIndex", "lastIndex", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    triggerContractByHeight(false, null, "contractName", "height", "apply", "adminPassword", "requireBlock", "requireLastBlock"),

    getDataTagCount(true, null, "chain", "requireBlock", "requireLastBlock"),

    bootstrapAPIProxy(false, null, "adminPassword"),

    dgsDelisting(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "goods", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    deleteCurrency(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAssetTransfers(false, null, "asset", "account", "firstIndex", "lastIndex", "timestamp", "includeAssetInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getBalance(true, null, "chain", "account", "height", "requireBlock", "requireLastBlock"),

    getCurrencyPhasedTransactions(true, null, "chain", "currency", "account", "withoutWhitelist", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    setPhasingOnlyControl(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "controlVotingModel", "controlQuorum", "controlMinBalance", "controlMinBalanceModel", "controlHolding", "controlWhitelisted", "controlWhitelisted", "controlWhitelisted", "controlSenderPropertySetter", "controlSenderPropertyName", "controlSenderPropertyValue", "controlRecipientPropertySetter", "controlRecipientPropertyName", "controlRecipientPropertyValue", "controlExpression", "controlMaxFees", "controlMaxFees", "controlMaxFees", "controlMinDuration", "controlMaxDuration", "controlParams", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getCurrencies(false, null, "currencies", "currencies", "currencies", "includeCounts", "requireBlock", "requireLastBlock"),

    getDGSGoods(true, null, "chain", "seller", "firstIndex", "lastIndex", "inStockOnly", "hideDelisted", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    currencyReserveIncrease(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "amountPerUnitNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    deleteAssetShares(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "quantityQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    setLogging(false, null, "logLevel", "communicationLogging", "adminPassword"),

    getAliasCount(true, null, "chain", "account", "requireBlock", "requireLastBlock"),

    getTransactionBytes(true, null, "chain", "fullHash", "requireBlock", "requireLastBlock"),

    retrievePrunedTransaction(true, null, "chain", "transactionFullHash"),

    getExpectedAssetTransfers(true, null, "chain", "asset", "account", "includeAssetInfo", "requireBlock", "requireLastBlock"),

    getAllAssets(false, null, "firstIndex", "lastIndex", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    hash(false, null, "hashAlgorithm", "secret", "secretIsText"),

    createPoll(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "name", "description", "finishHeight", "votingModel", "minNumberOfOptions", "maxNumberOfOptions", "minRangeValue", "maxRangeValue", "minBalance", "minBalanceModel", "holding", "option00", "option01", "option02", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    verifyPrunableMessage(true, null, "chain", "transactionFullHash", "message", "messageIsText", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "compressMessageToEncrypt", "requireBlock", "requireLastBlock"),

    getDGSPurchase(true, null, "chain", "purchase", "secretPhrase", "sharedKey", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getReferencingTransactions(true, null, "chain", "transactionFullHash", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getForging(false, null, "secretPhrase", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    readMessage(true, null, "chain", "transactionFullHash", "secretPhrase", "sharedKey", "retrieve", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    luceneReindex(false, null, "adminPassword"),

    deleteAssetProperty(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "property", "setter", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getExpectedCoinExchangeOrders(true, null, "chain", "exchange", "account", "requireBlock", "requireLastBlock"),

    fullReset(false, null, "adminPassword"),

    getAccountBlockIds(false, null, "account", "timestamp", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getPollResult(true, null, "chain", "poll", "votingModel", "holding", "minBalance", "minBalanceModel", "requireBlock", "requireLastBlock"),

    exchangeCoins(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "exchange", "quantityQNT", "priceNQTPerCoin", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getDGSPurchaseCount(true, null, "chain", "seller", "buyer", "withPublicFeedbacksOnly", "completed", "requireBlock", "requireLastBlock"),

    getAllWaitingTransactions(false, null, "requireBlock", "requireLastBlock"),

    decryptFrom(false, null, "account", "data", "nonce", "decryptedMessageIsText", "uncompressDecryptedMessage", "secretPhrase", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getAccountAssetCount(false, null, "account", "height", "requireBlock", "requireLastBlock"),

    getAssets(false, null, "assets", "assets", "assets", "includeCounts", "requireBlock", "requireLastBlock"),

    getCurrenciesByIssuer(false, null, "account", "account", "account", "firstIndex", "lastIndex", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    getBundlerRates(false, null, "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "transactionPriority"),

    getPeers(false, null, "active", "state", "service", "service", "service", "includePeerInfo", "version", "includeNewer", "connect", "adminPassword"),

    getAllShufflings(true, null, "chain", "includeFinished", "includeHoldingInfo", "finishedOnly", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    placeAskOrder(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "quantityQNT", "priceNQTPerShare", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    rebroadcastUnconfirmedTransactions(false, null, "adminPassword"),

    startBundler(true, null, "chain", "secretPhrase", "minRateNQTPerFXT", "totalFeesLimitFQT", "overpayFQTPerFXT", "feeCalculatorName", "filter", "filter", "filter", "bundlingRulesJSON", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getAllCurrencies(false, null, "firstIndex", "lastIndex", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    setAccountInfo(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "name", "description", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getDGSGood(true, null, "chain", "goods", "includeCounts", "requireBlock", "requireLastBlock"),

    getAskOrderIds(true, null, "chain", "asset", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountCurrencyCount(false, null, "account", "height", "requireBlock", "requireLastBlock"),

    getAskOrder(true, null, "chain", "order", "requireBlock", "requireLastBlock"),

    getFxtTransaction(false, null, "transaction", "fullHash", "includeChildTransactions", "requireBlock", "requireLastBlock"),

    getExpectedExchangeRequests(true, null, "chain", "account", "currency", "includeCurrencyInfo", "requireBlock", "requireLastBlock"),

    getCurrencyIds(false, null, "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    shufflingProcess(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "shufflingFullHash", "recipientSecretPhrase", "recipientPublicKey", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    requeueUnconfirmedTransactions(false, null, "adminPassword"),

    signTransaction(false, null, "unsignedTransactionJSON", "unsignedTransactionBytes", "prunableAttachmentJSON", "secretPhrase", "validate", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    deleteContractReference(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "contractName", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAliases(true, null, "chain", "timestamp", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    trimDerivedTables(false, null, "adminPassword"),

    getSellOffers(true, null, "chain", "currency", "account", "availableOnly", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAssetHistory(false, null, "asset", "account", "firstIndex", "lastIndex", "timestamp", "deletesOnly", "increasesOnly", "includeAssetInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getLog(false, null, "count", "adminPassword"),

    getCoinExchangeOrders(true, null, "chain", "exchange", "account", "firstIndex", "lastIndex", "showExpectedCancellations", "adminPassword", "requireBlock", "requireLastBlock"),

    getAccountLedgerEntry(false, null, "ledgerId", "includeTransaction", "includeHoldingInfo"),

    transferAsset(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "asset", "quantityQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    triggerContractByTransaction(true, null, "chain", "triggerFullHash", "apply", "validate", "adminPassword", "requireBlock", "requireLastBlock"),

    stopShuffler(false, null, "account", "shufflingFullHash", "secretPhrase", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    bundleTransactions(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "transactionFullHash", "transactionFullHash", "transactionFullHash", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getBalances(true, null, "chain", "chain", "chain", "account", "height", "requireBlock", "requireLastBlock"),

    getContractReferences(false, null, "account", "contractName", "includeContract", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    publishExchangeOffer(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "buyRateNQTPerUnit", "sellRateNQTPerUnit", "totalBuyLimitQNT", "totalSellLimitQNT", "initialBuySupplyQNT", "initialSellSupplyQNT", "expirationHeight", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getLinkedPhasedTransactions(false, null, "linkedFullHash", "requireBlock", "requireLastBlock"),

    triggerContractByRequest(false, null, "contractName", "setupParams", "adminPassword", "requireBlock", "requireLastBlock"),

    approveTransaction(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "phasedTransaction", "phasedTransaction", "phasedTransaction", "revealedSecret", "revealedSecret", "revealedSecret", "revealedSecretIsText", "revealedSecretText", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getDGSTagsLike(true, null, "chain", "tagPrefix", "inStockOnly", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    parseTransaction(false, null, "transactionJSON", "transactionBytes", "prunableAttachmentJSON", "requireBlock", "requireLastBlock"),

    getCurrency(true, null, "chain", "currency", "code", "includeCounts", "includeDeleted", "requireBlock", "requireLastBlock"),

    increaseAssetShares(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "quantityQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getBidOrders(true, null, "chain", "asset", "firstIndex", "lastIndex", "showExpectedCancellations", "adminPassword", "requireBlock", "requireLastBlock"),

    getCoinExchangeOrder(false, null, "order", "requireBlock", "requireLastBlock"),

    getDGSGoodsCount(true, null, "chain", "seller", "inStockOnly", "requireBlock", "requireLastBlock"),

    getCurrencyAccountCount(false, null, "currency", "height", "requireBlock", "requireLastBlock"),

    getDGSPurchases(true, null, "chain", "seller", "buyer", "firstIndex", "lastIndex", "withPublicFeedbacksOnly", "completed", "adminPassword", "requireBlock", "requireLastBlock"),

    getShufflingParticipants(true, null, "chain", "shufflingFullHash", "requireBlock", "requireLastBlock"),

    getAccountLessors(false, null, "account", "height", "requireBlock", "requireLastBlock"),

    setAssetProperty(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "asset", "property", "value", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getSupportedContracts(false, null, "adminPassword", "requireBlock", "requireLastBlock"),

    startShuffler(true, null, "chain", "secretPhrase", "shufflingFullHash", "recipientSecretPhrase", "recipientPublicKey", "feeRateNQTPerFXT", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    blacklistBundler(false, null, "account", "adminPassword"),

    getPoll(true, null, "chain", "poll", "requireBlock", "requireLastBlock"),

    getVoterPhasedTransactions(true, null, "chain", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    transferCurrency(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "recipient", "currency", "unitsQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    leaseBalance(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "period", "recipient", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    setAlias(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "aliasName", "aliasURI", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    shutdown(false, null, "scan", "adminPassword"),

    getHashedSecretPhasedTransactions(false, null, "phasingHashedSecret", "phasingHashedSecretAlgorithm", "requireBlock", "requireLastBlock"),

    getDGSExpiredPurchases(true, null, "chain", "seller", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    searchCurrencies(false, null, "query", "firstIndex", "lastIndex", "includeCounts", "adminPassword", "requireBlock", "requireLastBlock"),

    shufflingRegister(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "shufflingFullHash", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    currencyReserveClaim(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "unitsQNT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getPollVotes(true, null, "chain", "poll", "firstIndex", "lastIndex", "includeWeights", "adminPassword", "requireBlock", "requireLastBlock"),

    getStandbyShufflers(true, null, "chain", "secretPhrase", "holdingType", "holding", "account", "includeHoldingInfo", "adminPassword", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getAccountCurrentAskOrders(true, null, "chain", "account", "asset", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getDGSTags(true, null, "chain", "inStockOnly", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getOrderTrades(true, null, "chain", "askOrderFullHash", "bidOrderFullHash", "includeAssetInfo", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getConfiguration(false, null, "adminPassword"),

    getEpochTime(false, null, "unixtime"),

    sellAlias(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "alias", "aliasName", "recipient", "priceNQT", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    dumpPeers(false, null, "version", "includeNewer", "connect", "adminPassword", "service", "service", "service"),

    getAllOpenAskOrders(true, null, "chain", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getAllPrunableMessages(true, null, "chain", "firstIndex", "lastIndex", "timestamp", "adminPassword", "requireBlock", "requireLastBlock"),

    dgsFeedback(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "purchase", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getPhasingPoll(true, null, "chain", "transactionFullHash", "countVotes", "requireBlock", "requireLastBlock"),

    shufflingVerify(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "shufflingFullHash", "shufflingStateHash", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getDGSGoodsPurchases(true, null, "chain", "goods", "buyer", "firstIndex", "lastIndex", "withPublicFeedbacksOnly", "completed", "adminPassword", "requireBlock", "requireLastBlock"),

    getAssetAccountCount(false, null, "asset", "height", "requireBlock", "requireLastBlock"),

    getPhasingPollVotes(true, null, "chain", "transactionFullHash", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    retrievePrunedData(true, null, "chain", "adminPassword"),

    getUnconfirmedTransactions(true, null, "chain", "account", "account", "account", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    encryptTo(false, null, "recipient", "messageToEncrypt", "messageToEncryptIsText", "compressMessageToEncrypt", "secretPhrase", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getBuyOffers(true, null, "chain", "currency", "account", "availableOnly", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getState(true, null, "chain", "includeCounts", "adminPassword"),

    issueCurrency(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "name", "code", "description", "type", "initialSupplyQNT", "reserveSupplyQNT", "maxSupplyQNT", "issuanceHeight", "minReservePerUnitNQT", "minDifficulty", "maxDifficulty", "ruleset", "algorithm", "decimals", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAccountId(false, null, "secretPhrase", "publicKey", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    getCoinExchangeTrade(false, null, "orderFullHash", "matchFullHash", "requireBlock", "requireLastBlock"),

    issueAsset(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "name", "description", "quantityQNT", "decimals", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    combineSecret(false, null, "pieces", "pieces", "pieces"),

    getTrades(true, null, "chain", "asset", "account", "firstIndex", "lastIndex", "timestamp", "includeAssetInfo", "adminPassword", "requireBlock", "requireLastBlock"),

    getPrunableMessages(true, null, "chain", "account", "otherAccount", "secretPhrase", "firstIndex", "lastIndex", "timestamp", "adminPassword", "requireBlock", "requireLastBlock", "privateKey", "sharedPieceAccount", "sharedPiece", "sharedPiece", "sharedPiece"),

    calculateFullHash(false, null, "unsignedTransactionBytes", "unsignedTransactionJSON", "signatureHash"),

    currencyMint(true, new String[] {"messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "currency", "nonce", "unitsQNT", "counter", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    uploadTaggedData(true, new String[] {"file", "messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"}, "chain", "name", "description", "tags", "type", "channel", "isText", "filename", "data", "secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT", "deadline", "referencedTransaction", "broadcast", "timestamp", "message", "messageIsText", "messageIsPrunable", "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt", "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf", "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel", "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls", "phasingSenderPropertySetter", "phasingSenderPropertyName", "phasingSenderPropertyValue", "phasingRecipientPropertySetter", "phasingRecipientPropertyName", "phasingRecipientPropertyValue", "phasingExpression", "recipientPublicKey", "ecBlockId", "ecBlockHeight", "voucher", "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount", "transactionPriority"),

    getAccountProperties(false, null, "recipient", "property", "setter", "firstIndex", "lastIndex", "adminPassword", "requireBlock", "requireLastBlock"),

    getExchanges(true, null, "chain", "currency", "account", "firstIndex", "lastIndex", "timestamp", "includeCurrencyInfo", "adminPassword", "requireBlock", "requireLastBlock");

    private final boolean isChainSpecific;

    private final List<String> fileParameters;

    private final List<String> parameters;

    ApiSpec(boolean isChainSpecific, String[] fileParameters, String... parameters) {
        this.isChainSpecific = isChainSpecific;
        this.fileParameters = fileParameters == null ? Collections.emptyList() : Arrays.asList(fileParameters);
        this.parameters = Arrays.asList(parameters);}

    public boolean isChainSpecific() {
        return isChainSpecific;
    }

    public List<String> getFileParameters() {
        return fileParameters;
    }

    public List<String> getParameters() {
        return parameters;
    }
}
