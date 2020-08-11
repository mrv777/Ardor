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

package nxt.http;

import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.account.Account;
import nxt.account.PublicKeyAnnouncementAppendix;
import nxt.blockchain.Appendix;
import nxt.blockchain.Attachment;
import nxt.blockchain.Chain;
import nxt.blockchain.ChainTransactionId;
import nxt.blockchain.ChildChain;
import nxt.blockchain.ChildTransaction;
import nxt.blockchain.ChildTransactionType;
import nxt.blockchain.FxtChain;
import nxt.blockchain.FxtTransactionType;
import nxt.blockchain.Transaction;
import nxt.crypto.Crypto;
import nxt.messaging.EncryptToSelfMessageAppendix;
import nxt.messaging.MessageAppendix;
import nxt.messaging.PrunablePlainMessageAppendix;
import nxt.peer.FeeRateCalculator;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import nxt.voting.PhasingAppendix;
import nxt.voting.PhasingParams;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static nxt.http.JSONResponses.INCORRECT_EC_BLOCK;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE_OR_PRIVATE_KEY;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;
import static nxt.peer.FeeRateCalculator.TransactionPriority.NORMAL;

public abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[]{"secretPhrase", "privateKey", "publicKey", "feeNQT", "feeRateNQTPerFXT", "minBundlerBalanceFXT", "minBundlerFeeLimitFQT",
            "deadline", "referencedTransaction", "broadcast", "timestamp",
            "message", "messageIsText", "messageIsPrunable",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf",
            "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel",
            "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted",
            "phasingLinkedTransaction", "phasingLinkedTransaction", "phasingLinkedTransaction",
            "phasingHashedSecret", "phasingHashedSecretAlgorithm", "phasingParams", "phasingSubPolls",
            "phasingSenderPropertySetter", "phasingSenderPropertyName",
            "phasingSenderPropertyValue", "phasingRecipientPropertySetter",
            "phasingRecipientPropertyName", "phasingRecipientPropertyValue",
            "phasingExpression",
            "recipientPublicKey",
            "ecBlockId", "ecBlockHeight", "voucher",
            "sharedPiece", "sharedPiece", "sharedPiece", "sharedPieceAccount",
            "transactionPriority"
    };

    private static final List<String> commonFileParameters = Collections.unmodifiableList(Arrays.asList(
            "messageFile", "messageToEncryptFile", "encryptToSelfMessageFile", "encryptedMessageFile"
    ));

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    public static List<String> getCommonParameters() {
        return Collections.unmodifiableList(Arrays.asList(commonParameters));
    }

    private static List<String> addCommonFileParameters(List<String> fileParameters) {
        List<String> result = new ArrayList<>(fileParameters);
        result.addAll(commonFileParameters);
        return result;
    }

    public static List<String> getCommonFileParameters() {
        return commonFileParameters;
    }

    CreateTransaction(APITag[] apiTags, String... parameters) {
        this(Collections.emptyList(), apiTags, parameters);
    }

    protected CreateTransaction(List<String> fileParameters, APITag[] apiTags, String... parameters) {
        super(addCommonFileParameters(fileParameters), apiTags, addCommonParameters(parameters));
        if (!getAPITags().contains(APITag.CREATE_TRANSACTION)) {
            throw new RuntimeException("CreateTransaction API " + getClass().getName() + " is missing APITag.CREATE_TRANSACTION tag");
        }
    }

    protected final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
            throws NxtException {
        return transactionParameters(req, senderAccount, attachment).createTransaction();
    }

    protected final CreateTransactionParameters transactionParameters(HttpServletRequest req, Account senderAccount, Attachment attachment) {
        return new CreateTransactionParameters(req).setSenderAccount(senderAccount).setAttachment(attachment);
    }

    private PhasingAppendix parsePhasing(HttpServletRequest req) throws ParameterException {
        int finishHeight = ParameterParser.getInt(req, "phasingFinishHeight",
                Nxt.getBlockchain().getHeight() + 1,
                Nxt.getBlockchain().getHeight() + Constants.MAX_PHASING_DURATION + 1,
                true);
        JSONObject phasingParamsJson = ParameterParser.getJson(req, "phasingParams");
        PhasingParams phasingParams;
        if (phasingParamsJson != null) {
            phasingParams = new PhasingParams(phasingParamsJson);
        } else {
            phasingParams = ParameterParser.parsePhasingParams(req, "phasing");
        }
        return new PhasingAppendix(finishHeight, phasingParams);
    }

    private JSONStreamAware createTransactionFromParameters(CreateTransactionParameters parameters) throws NxtException {
        final HttpServletRequest req = parameters.getReq();
        final ChainTransactionId referencedTransactionId = parameters.getReferencedTransactionId();
        final Attachment attachment = parameters.getAttachment();
        final long amountNQT = parameters.getAmountNQT();
        final long recipientId = parameters.getRecipientId();
        final long senderId = parameters.getSenderId() == 0 ? parameters.getSenderAccount().getId() : parameters.getSenderId();

        byte[] privateKey = ParameterParser.getPrivateKey(req, false);
        boolean isVoucher = "true".equalsIgnoreCase(req.getParameter("voucher"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast")) && privateKey != null && !isVoucher;
        Appendix encryptedMessage = null;
        if (attachment.getTransactionType().canHaveRecipient() && recipientId != 0) {
            Account recipient = Account.getAccount(recipientId);
            encryptedMessage = ParameterParser.getEncryptedMessage(req, recipient);
        }
        EncryptToSelfMessageAppendix encryptToSelfMessage = ParameterParser.getEncryptToSelfMessage(req);
        Appendix message = parameters.getMessage();

        PublicKeyAnnouncementAppendix publicKeyAnnouncement = null;
        String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
        if (recipientPublicKey != null) {
            publicKeyAnnouncement = new PublicKeyAnnouncementAppendix(Convert.parseHexString(recipientPublicKey));
        }

        PhasingAppendix phasing = null;
        boolean phased = "true".equalsIgnoreCase(req.getParameter("phased"));
        if (phased) {
            phasing = parsePhasing(req);
        }

        if (privateKey == null && publicKeyValue == null) {
            return MISSING_SECRET_PHRASE_OR_PRIVATE_KEY;
        }

        short deadline = parameters.getDeadline();
        long feeNQT = ParameterParser.getLong(req, "feeNQT", -1L, Constants.MAX_BALANCE_NQT, -1L);
        int ecBlockHeight = ParameterParser.getInt(req, "ecBlockHeight", 0, Integer.MAX_VALUE, false);
        long ecBlockId = Convert.parseUnsignedLong(req.getParameter("ecBlockId"));
        if (ecBlockId != 0 && ecBlockId != Nxt.getBlockchain().getBlockIdAtHeight(ecBlockHeight)) {
            return INCORRECT_EC_BLOCK;
        }
        if (ecBlockId == 0 && ecBlockHeight > 0) {
            ecBlockId = Nxt.getBlockchain().getBlockIdAtHeight(ecBlockHeight);
        }
        int timestamp = ParameterParser.getTimestamp(req);
        long feeRateNQTPerFXT = ParameterParser.getLong(req, "feeRateNQTPerFXT", -1L, Constants.MAX_BALANCE_NQT, -1L);
        JSONObject response = new JSONObject();

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = privateKey != null && !isVoucher ? Crypto.getPublicKey(privateKey) : Convert.parseHexString(publicKeyValue);

        // Allow the caller to specify the chain for the transaction instead of using
        // the 'chain' request parameter
        Chain chain = parameters.getChain();

        if (feeNQT < 0L && feeRateNQTPerFXT < 0L && chain != FxtChain.FXT) {
            FeeRateCalculator feeRateCalculator = FeeRateCalculator.create()
                    .setMinBalance(ParameterParser.getLong(req, "minBundlerBalanceFXT", 0, Constants.MAX_BALANCE_FXT, Constants.minBundlerBalanceFXT))
                    .setMinFeeLimit(ParameterParser.getLong(req, "minBundlerFeeLimitFQT", 0, Constants.MAX_BALANCE_FXT * Constants.ONE_FXT, Constants.minBundlerFeeLimitFXT * Constants.ONE_FXT))
                    .setPriority(ParameterParser.getPriority(req, "transactionPriority", NORMAL))
                    .build();
            feeRateNQTPerFXT = feeRateCalculator.getBestRate(chain);
            broadcast = false;
            if (!isVoucher) {
                response.put("bundlerRateNQTPerFXT", String.valueOf(feeRateNQTPerFXT));
            }
        }

        try {
            Transaction.Builder builder = chain.newTransactionBuilder(publicKey, amountNQT, feeNQT, deadline, attachment);
            if (chain instanceof ChildChain) {
                if (!(attachment.getTransactionType() instanceof ChildTransactionType)) {
                    throw new ParameterException(JSONResponses.incorrect("chain",
                            attachment.getTransactionType().getName() + " attachment not allowed for "
                                    + chain.getName() + " chain"));
                }
                builder = ((ChildTransaction.Builder) builder)
                        .referencedTransaction(referencedTransactionId)
                        .feeRateNQTPerFXT(feeRateNQTPerFXT)
                        .appendix(publicKeyAnnouncement)
                        .appendix(encryptToSelfMessage)
                        .appendix(phasing);
            } else {
                if (!(attachment.getTransactionType() instanceof FxtTransactionType)) {
                    throw new ParameterException(JSONResponses.incorrect("chain",
                            attachment.getTransactionType().getName() + " attachment not allowed for "
                                    + chain.getName() + " chain"));
                }
                if (referencedTransactionId != null) {
                    return JSONResponses.error("Referenced transactions not allowed for Ardor transactions");
                }
                if (encryptedMessage != null && !(encryptedMessage instanceof Appendix.Prunable)) {
                    return JSONResponses.error("Permanent encrypted message attachments not allowed for Ardor transactions");
                }
                if (message != null && !(message instanceof Appendix.Prunable)) {
                    return JSONResponses.error("Permanent message attachments not allowed for Ardor transactions");
                }
                if (publicKeyAnnouncement != null) {
                    return JSONResponses.error("Public key announcement attachments not allowed for Ardor transactions");
                }
                if (encryptToSelfMessage != null) {
                    return JSONResponses.error("Encrypted to self message attachments not allowed for Ardor transactions");
                }
                if (phasing != null) {
                    return JSONResponses.error("Phasing attachments not allowed for Ardor transactions");
                }
            }
            builder.appendix(encryptedMessage)
                    .appendix(message);
            if (attachment.getTransactionType().canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            if (ecBlockId != 0) {
                builder.ecBlockId(ecBlockId);
                builder.ecBlockHeight(ecBlockHeight);
            }
            if (timestamp > 0) {
                builder.timestamp(timestamp);
            }
            Transaction transaction = builder.build(privateKey, isVoucher);
            try {
                long balance = chain.getBalanceHome().getBalance(senderId).getUnconfirmedBalance();
                if (Math.addExact(amountNQT, transaction.getFee()) > balance) {
                    JSONObject infoJson = new JSONObject();
                    infoJson.put("errorCode", 6);
                    infoJson.put("errorDescription", "Not enough funds");
                    infoJson.put("amount", String.valueOf(amountNQT));
                    infoJson.put("fee", String.valueOf(transaction.getFee()));
                    infoJson.put("balance", String.valueOf(balance));
                    infoJson.put("diff", String.valueOf(Math.subtractExact(Math.addExact(amountNQT, transaction.getFee()), balance)));
                    infoJson.put("chain", chain.getId());
                    return JSON.prepare(infoJson);
                }
            } catch (ArithmeticException e) {
                Logger.logErrorMessage(String.format("amount %d fee %d", amountNQT, transaction.getFee()), e);
                return NOT_ENOUGH_FUNDS;
            }
            JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
            if (isVoucher) {
                transactionJSON.remove("fullHash");
                transactionJSON.put("signature", null);
            }
            response.put("transactionJSON", transactionJSON);
            try {
                response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
            } catch (NxtException.NotYetEncryptedException ignore) {}
            if (privateKey != null) {
                if (isVoucher) {
                    response.put("signature", Convert.toHexString(transaction.getSignature()));
                    response.put("publicKey", Convert.toHexString(Crypto.getPublicKey(privateKey)));
                    response.put("requestType", req.getParameter("requestType"));
                    ParameterParser.parseVoucher(response);
                } else {
                    response.put("fullHash", transactionJSON.get("fullHash"));
                    response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                    response.put("signatureHash", transactionJSON.get("signatureHash"));
                }
            }
            if (!isVoucher) {
                response.put("minimumFeeFQT", String.valueOf(transaction.getMinimumFeeFQT()));
            }
            if (broadcast) {
                Nxt.getTransactionProcessor().broadcast(transaction);
                response.put("broadcasted", true);
            } else {
                transaction.validate();
                if (!isVoucher) {
                    response.put("broadcasted", false);
                }
            }
        } catch (NxtException.NotYetEnabledException e) {
            return FEATURE_NOT_AVAILABLE;
        } catch (NxtException.InsufficientBalanceException e) {
            throw e;
        } catch (NxtException.ValidationException e) {
            if (broadcast) {
                response.clear();
            }
            response.put("broadcasted", false);
            JSONData.putException(response, e);
        }
        return response;

    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected final boolean isChainSpecific() {
        return true;
    }

    protected class CreateTransactionParameters {
        private final HttpServletRequest req;
        private Account senderAccount;
        private long senderId;
        private long recipientId;
        private long amountNQT;
        private short deadline;
        private Attachment attachment;
        private Chain txChain;
        private ChainTransactionId referencedTransactionId;
        private Appendix message;

        protected CreateTransactionParameters(HttpServletRequest req) {
            this.req = req;
        }

        public CreateTransactionParameters setSenderAccount(Account senderAccount) {
            this.senderAccount = senderAccount;
            return this;
        }

        public CreateTransactionParameters setSenderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        public CreateTransactionParameters setRecipientId(long recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public CreateTransactionParameters setAmountNQT(long amountNQT) {
            this.amountNQT = amountNQT;
            return this;
        }

        public CreateTransactionParameters setDeadline(short deadline) {
            this.deadline = deadline;
            return this;
        }

        public CreateTransactionParameters setAttachment(Attachment attachment) {
            this.attachment = attachment;
            return this;
        }

        public CreateTransactionParameters setTxChain(Chain txChain) {
            this.txChain = txChain;
            return this;
        }

        public CreateTransactionParameters setReferencedTransactionId(ChainTransactionId referencedTransactionId) {
            this.referencedTransactionId = referencedTransactionId;
            return this;
        }

        public CreateTransactionParameters setMessage(String message, boolean messageIsText, boolean isPrunable) {
            this.message = isPrunable ? new PrunablePlainMessageAppendix(message, messageIsText)
                    : new MessageAppendix(message, messageIsText);
            return this;
        }

        public HttpServletRequest getReq() {
            return req;
        }

        public Chain getChain() throws ParameterException {
            if (txChain != null) {
                return txChain;
            }
            return ParameterParser.getChain(req);
        }

        public ChainTransactionId getReferencedTransactionId() throws ParameterException {
            if (referencedTransactionId != null) {
                return referencedTransactionId;
            }
            return ParameterParser.getChainTransactionId(req, "referencedTransaction");
        }

        public Account getSenderAccount() throws ParameterException {
            if (senderAccount != null) {
                return senderAccount;
            }
            return ParameterParser.getSenderAccount(req);
        }

        public long getSenderId() {
            return senderId;
        }

        public Attachment getAttachment() {
            return attachment;
        }

        public long getAmountNQT() {
            return amountNQT;
        }

        public long getRecipientId() {
            return recipientId;
        }

        protected final JSONStreamAware createTransaction() throws NxtException {
            return createTransactionFromParameters(this);
        }

        public Appendix getMessage() throws ParameterException {
            if (message != null) {
                return message;
            }
            return ParameterParser.getPlainMessage(req);
        }

        public short getDeadline() throws ParameterException {
            if (deadline != 0) {
                return deadline;
            }
            return (short) ParameterParser.getInt(req, "deadline", 1, Short.MAX_VALUE, 15);
        }
    }
}
