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

package nxt.http.responses;

import nxt.NxtException;
import nxt.addons.ContractRunnerConfig;
import nxt.addons.JO;
import nxt.blockchain.Chain;
import nxt.blockchain.ChainTransactionId;
import nxt.blockchain.FxtChain;
import nxt.blockchain.Transaction;
import nxt.blockchain.TransactionType;
import nxt.http.ParameterException;
import nxt.http.ParameterParser;
import nxt.messaging.PrunableEncryptedMessageAppendix;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Objects;

public class TransactionResponseImpl implements TransactionResponse {

    private final JO transactionJson;
    private final long fxtTransaction;
    private final long transactionId;
    private final int confirmations;

    private final TransactionType transactionType;
    private final int chainId;
    private final long senderId;
    private final byte[] senderPublicKey;
    private final long recipientId;
    private final int height;
    private final long blockId;
    private final short index;
    private final int timestamp;
    private final int blockTimeStamp;
    private final short deadline;
    private final long amount;
    private final long fee;
    private final ChainTransactionId referencedTransaction;
    private final byte[] signature;
    private final byte[] signatureHash;
    private final byte[] fullHash;
    private final byte version;
    private final int ecBlockHeight;
    private final long ecBlockId;
    private final boolean isPhased;
    private final boolean isBundled;
    private final PrunableEncryptedMessageAppendix encryptedAppendix;

    TransactionResponseImpl(JSONObject response) {
        this(new JO(response));
    }

    TransactionResponseImpl(JO response) {
        this.transactionJson = response;
        transactionType = TransactionType.findTransactionType(transactionJson.getByte("type"), transactionJson.getByte("subtype"));
        chainId = transactionJson.getInt("chain");
        isPhased = transactionJson.getBoolean("phased");
        timestamp = transactionJson.getInt("timestamp");
        deadline = transactionJson.getShort("deadline");
        senderPublicKey = Convert.parseHexString(transactionJson.getString("senderPublicKey"));
        if (transactionJson.isExist("recipient")) {
            recipientId = transactionJson.getEntityId("recipient");
        } else {
            recipientId = 0;
        }
        amount = transactionJson.getLong("amountNQT");
        fee = transactionJson.getLong("feeNQT");
        if (Chain.getChain(chainId) != FxtChain.FXT) {
            if (transactionJson.isExist("referencedTransaction")) {
                JO referencedTransactionJson = new JO(transactionJson.get("referencedTransaction"));
                referencedTransaction = new ChainTransactionId(referencedTransactionJson.getInt("chain"), referencedTransactionJson.parseHexString("transactionFullHash"));
            } else {
                referencedTransaction = null;
            }
            fxtTransaction = transactionJson.getEntityId("fxtTransaction");
            if (transactionJson.isExist("isBundled")) {
                isBundled = transactionJson.getBoolean("isBundled");
            } else {
                isBundled = false;
            }
        } else {
            fxtTransaction = 0;
            referencedTransaction = null;
            isBundled = false;
        }
        if (transactionJson.isExist("signature")) {
            signature = transactionJson.parseHexString("signature");
            signatureHash = transactionJson.parseHexString("signatureHash");
            fullHash = transactionJson.parseHexString("fullHash");
            if (Chain.getChain(chainId) == FxtChain.FXT) {
                transactionId = transactionJson.getEntityId("transaction");
            } else {
                BigInteger bigInteger = new BigInteger(1, new byte[]{fullHash[7], fullHash[6], fullHash[5], fullHash[4], fullHash[3], fullHash[2], fullHash[1], fullHash[0]});
                transactionId = bigInteger.longValue();
            }
        } else {
            signature = null;
            signatureHash = null;
            fullHash = null;
            transactionId = 0;
        }
        senderId = transactionJson.getEntityId("sender");
        if (transactionJson.isExist("height")) {
            height = transactionJson.getInt("height");
        } else {
            height = Integer.MAX_VALUE;
        }
        version = transactionJson.getByte("version");
        ecBlockId = transactionJson.getEntityId("ecBlockId");
        ecBlockHeight = transactionJson.getInt("ecBlockHeight");
        if (transactionJson.isExist("block")) {
            blockId = transactionJson.getEntityId("block");
            confirmations = transactionJson.getInt("confirmations");
            blockTimeStamp = transactionJson.getInt("blockTimestamp");
            index = transactionJson.getShort("transactionIndex");
        } else {
            blockId = 0;
            confirmations = -1;
            blockTimeStamp = 0;
            index = -1;
        }
        JO attachment = transactionJson.getJo("attachment");
        if (attachment != null && attachment.isExist("encryptedMessage") && attachment.isExist("version.PrunableEncryptedMessage")) {
            encryptedAppendix = new PrunableEncryptedMessageAppendix(attachment.toJSONObject());
        } else {
            encryptedAppendix = null;
        }
    }

    @Override
    public int getChainId() {
        return chainId;
    }

    @Override
    public long getSenderId() {
        return senderId;
    }

    @Override
    public String getSender() {
        return Long.toUnsignedString(senderId);
    }

    @Override
    public String getSenderRs() {
        if (senderId == 0) {
            return null;
        }
        return Convert.rsAccount(senderId);
    }

    @Override
    public byte[] getSenderPublicKey() {
        return senderPublicKey;
    }

    @Override
    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public String getRecipient() {
        return Long.toUnsignedString(recipientId);
    }

    @Override
    public String getRecipientRs() {
        if (recipientId == 0) {
            return null;
        }
        return Convert.rsAccount(recipientId);
    }

    @Override
    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public long getBlockId() {
        return blockId;
    }

    @Override
    public short getIndex() {
        return index;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getBlockTimestamp() {
        return blockTimeStamp;
    }

    @Override
    public short getDeadline() {
        return deadline;
    }

    @Override
    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getFee() {
        return fee;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public byte[] getFullHash() {
        return fullHash;
    }

    @Override
    public byte getType() {
        return transactionType.getType();
    }

    @Override
    public byte getSubType() {
        return transactionType.getType();
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public int getECBlockHeight() {
        return ecBlockHeight;
    }

    @Override
    public long getECBlockId() {
        return ecBlockId;
    }

    @Override
    public boolean isPhased() {
        return isPhased;
    }

    @Override
    public long getFxtTransaction() {
        return fxtTransaction;
    }

    @Override
    public long getTransactionId() {
        return transactionId;
    }

    @Override
    public String getUnsignedLongTransactionId() {
        return Long.toUnsignedString(transactionId);
    }

    @Override
    public int getConfirmations() {
        return confirmations;
    }

    @Override
    public int getBlockTimeStamp() {
        return blockTimeStamp;
    }

    @Override
    public ChainTransactionId getReferencedTransaction() {
        return referencedTransaction;
    }

    @Override
    public byte[] getSignatureHash() {
        return signatureHash;
    }

    @Override
    public int getEcBlockHeight() {
        return ecBlockHeight;
    }

    @Override
    public long getEcBlockId() {
        return ecBlockId;
    }

    @Override
    public boolean isBundled() {
        return isBundled;
    }

    @Override
    public long getRandomSeed(ContractRunnerConfig config) {
        if (encryptedAppendix == null) {
            Logger.logWarningMessage("Transaction does not include encrypted message attachments, random values can be reproduced");
            return 0;
        }
        String messageText = Convert.toString(config.decryptFrom(senderPublicKey, encryptedAppendix.getEncryptedData(), encryptedAppendix.isCompressed()), encryptedAppendix.isText());
        JO messageObject;
        try {
            messageObject = JO.parse(messageText);
        } catch (Exception e) {
            Logger.logWarningMessage("Cannot parse secret seed, random values can be reproduced");
            return 0;
        }
        if (messageObject.isExist("seed")) {
            return Convert.bytesToLong(messageObject.parseHexString("seed"));
        }
        Logger.logWarningMessage("Transaction encrypted message attachement does not specify seed parameter, random values can be reproduced");
        return 0;
    }

    @Override
    public JO getAttachmentJson() {
        return transactionJson.getJo("attachment");
    }

    @Override
    public JO getJson() {
        return transactionJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            TransactionResponseImpl that = (TransactionResponseImpl)o;
            byte[] thisBytes;
            try {
                Transaction.Builder builder = ParameterParser.parseTransaction(transactionJson.toJSONString(), null, null);
                Transaction transaction = builder.build();
                thisBytes = transaction.getBytes();
            } catch (ParameterException | NxtException.NotValidException e) {
                throw new IllegalStateException(e);
            }
            byte[] thatBytes;
            try {
                Transaction.Builder builder = ParameterParser.parseTransaction(that.getTransactionJson().toJSONString(), null, null);
                Transaction transaction = builder.build();
                thatBytes = transaction.getBytes();
            } catch (ParameterException | NxtException.NotValidException e) {
                throw new IllegalStateException(e);
            }
            return Arrays.equals(thisBytes, thatBytes);
        });
    }

    @Override
    public boolean similar(TransactionResponse transactionResponse) {
        TransactionResponseImpl that = (TransactionResponseImpl)transactionResponse;
        if (chainId != that.chainId) {
            logDiffMessage("chainId", "" + chainId, "" + that.chainId);
            return false;
        }
        if (senderId != that.senderId) {
            logDiffMessage("senderId", "" + senderId, "" + that.senderId);
            return false;
        }
        if (recipientId != that.recipientId) {
            logDiffMessage("recipientId", "" + recipientId, "" + that.recipientId);
            return false;
        }
        if (amount != that.amount) {
            logDiffMessage("amount", "" + amount, "" + that.amount);
            return false;
        }
        if (fee != that.fee) {
            logDiffMessage("fee", "" + fee, "" + that.fee);
            return false;
        }
        if (version != that.version) {
            logDiffMessage("version", "" + version, "" + that.version);
            return false;
        }
        if (isPhased != that.isPhased) {
            logDiffMessage("isPhased", "" + isPhased, "" + that.isPhased);
            return false;
        }
        if (!Objects.equals(transactionType, that.transactionType)) {
            logDiffMessage("transactionType", "" + transactionType, "" + that.transactionType);
            return false;
        }
        if (!Arrays.equals(senderPublicKey, that.senderPublicKey)) {
            logDiffMessage("senderPublicKey", "" + Arrays.toString(senderPublicKey), "" + Arrays.toString(that.senderPublicKey));
            return false;
        }
        if (!Objects.equals(referencedTransaction, that.referencedTransaction)) {
            logDiffMessage("referencedTransaction", "" + referencedTransaction, "" + that.referencedTransaction);
            return false;
        }
        if (!getAttachmentJson().remove("message").equals(that.getAttachmentJson().remove("message"))) {
            logDiffMessage("attachmentJson", getAttachmentJson().toJSONString(), that.getAttachmentJson().toJSONString());
            return false;
        }
        return true;
    }

    private JO getTransactionJson() {
        return transactionJson;
    }

    private void logDiffMessage(String title, String myValue, String theirValue) {
        Logger.logInfoMessage("Value %s differ this %s that %s", title, myValue, theirValue);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(chainId, senderId, recipientId, amount, fee, version, isPhased, transactionType, senderPublicKey, referencedTransaction);
        result = 31 * result + Arrays.hashCode(senderPublicKey);
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Arrays.hashCode(signatureHash);
        result = 31 * result + Arrays.hashCode(fullHash);
        return result;
    }
}
