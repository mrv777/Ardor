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

package nxt.addons;

import nxt.Nxt;
import nxt.account.Account;
import nxt.account.AccountLedger;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildTransaction;
import nxt.blockchain.Transaction;
import nxt.crypto.Crypto;
import nxt.messaging.EncryptToSelfMessageAppendix;
import nxt.messaging.EncryptedMessageAppendix;
import nxt.messaging.MessageAppendix;
import nxt.messaging.PrunableMessageHome;
import nxt.util.Convert;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LedgerLogger extends AbstractLedgerLogger {

    private final char SEPARATOR = ',';
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private final long from;
    private final long to;
    {
        String s = Nxt.getStringProperty("nxt.LedgerLogger.from");
        try {
            from = s == null ? 0 : Convert.toEpochTime(dateFormat.parse(s).getTime());
        } catch (ParseException e) {
            throw new RuntimeException("Invalid nxt.LedgerLogger.from date", e);
        }
        s = Nxt.getStringProperty("nxt.LedgerLogger.to");
        try {
            to = s == null ? Long.MAX_VALUE : Convert.toEpochTime(dateFormat.parse(s).getTime());
        } catch (ParseException e) {
            throw new RuntimeException("Invalid nxt.LedgerLogger.to date", e);
        }
    }
    private final Map<String, byte[]> privateKeys = new HashMap<>();
    private final Map<byte[], byte[]> publicKeys = new HashMap<>();

    protected final String getColumnNames() {
        return "height,timestamp,date,account,ledger_event,holding,holding_id,chain,amount,balance,transaction,sender,recipient,transaction_type,message,encrypted_message,note";
    }

    protected final String getLogLine(AccountLedger.LedgerEntry ledgerEntry) {
        int timestamp = ledgerEntry.getTimestamp();
        if (timestamp < from || timestamp > to) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        buf.append(ledgerEntry.getHeight()).append(SEPARATOR);
        buf.append(ledgerEntry.getTimestamp()).append(SEPARATOR);
        buf.append(dateFormat.format(new Date(Convert.fromEpochTime(timestamp)))).append(SEPARATOR);
        String rsAccount = Convert.rsAccount(ledgerEntry.getAccountId());
        byte[] privateKey = getPrivateKey(rsAccount);
        byte[] myPublicKey = getPublicKey(privateKey);
        buf.append(rsAccount).append(SEPARATOR);
        buf.append(ledgerEntry.getEvent().name()).append(SEPARATOR);
        buf.append(ledgerEntry.getHolding().name()).append(SEPARATOR);
        buf.append(quote(Long.toUnsignedString(ledgerEntry.getHoldingId()))).append(SEPARATOR);
        buf.append(Chain.getChain(ledgerEntry.getChainId()).getName()).append(SEPARATOR);
        buf.append(BigDecimal.valueOf(ledgerEntry.getChange(), ledgerEntry.getHolding().getHoldingType().getDecimals(ledgerEntry.getHoldingId())).toPlainString()).append(SEPARATOR);
        buf.append(BigDecimal.valueOf(ledgerEntry.getBalance(), ledgerEntry.getHolding().getHoldingType().getDecimals(ledgerEntry.getHoldingId())).toPlainString()).append(SEPARATOR);
        if (ledgerEntry.getEvent().isTransaction()) {
            Transaction transaction = Chain.getChain(ledgerEntry.getChainId()).getTransactionHome().findTransaction(ledgerEntry.getEventHash());
            buf.append(transaction.getStringId()).append(SEPARATOR);
            long senderId = transaction.getSenderId();
            buf.append(Convert.rsAccount(senderId)).append(SEPARATOR);
            long recipientId = transaction.getRecipientId();
            buf.append(recipientId != 0 ? Convert.rsAccount(recipientId) : "").append(SEPARATOR);
            buf.append(transaction.getType().getName()).append(SEPARATOR);
            String message = null;
            String encryptedMessage = null;
            String encryptToSelfMessage = null;
            MessageAppendix messageAppendix = transaction instanceof ChildTransaction ? ((ChildTransaction)transaction).getMessage() : null;
            EncryptedMessageAppendix encryptedMessageAppendix = transaction instanceof ChildTransaction ? ((ChildTransaction)transaction).getEncryptedMessage() : null;
            EncryptToSelfMessageAppendix encryptToSelfMessageAppendix = transaction instanceof ChildTransaction ? ((ChildTransaction)transaction).getEncryptToSelfMessage() : null;
            if (messageAppendix != null) {
                message = quote(Convert.toString(messageAppendix.getMessage(), messageAppendix.isText()));
            }
            if (encryptedMessageAppendix != null) {
                if (privateKey == null) {
                    encryptedMessage = "ENCRYPTED";
                } else {
                    byte[] senderPublicKey = Account.getPublicKey(transaction.getSenderId());
                    byte[] recipientPublicKey = Account.getPublicKey(transaction.getRecipientId());
                    byte[] publicKey = Arrays.equals(senderPublicKey, myPublicKey) ? recipientPublicKey : senderPublicKey;
                    if (publicKey != null) {
                        try {
                            encryptedMessage = quote(Convert.toString(
                                    Account.decryptFrom(privateKey, publicKey, encryptedMessageAppendix.getEncryptedData(), encryptedMessageAppendix.isCompressed()),
                                    encryptedMessageAppendix.isText()));
                        } catch (RuntimeException e) {
                            encryptedMessage = "UNDECRYPTABLE";
                        }
                    } else {
                        encryptedMessage = "UNDECRYPTABLE";
                    }
                }
            }
            if (encryptToSelfMessageAppendix != null) {
                if (privateKey == null) {
                    encryptToSelfMessage = "ENCRYPTED";
                } else {
                    try {
                        byte[] decrypted = Account.decryptFrom(privateKey, myPublicKey, encryptToSelfMessageAppendix.getEncryptedData(), encryptToSelfMessageAppendix.isCompressed());
                        encryptToSelfMessage = quote(Convert.toString(decrypted, encryptToSelfMessageAppendix.isText()));
                    } catch (RuntimeException e) {
                        encryptToSelfMessage = "UNDECRYPTABLE";
                    }
                }
            }
            if (message == null && encryptedMessage == null) {
                Chain chain = transaction.getChain();
                PrunableMessageHome prunableMessageHome = chain.getPrunableMessageHome();
                PrunableMessageHome.PrunableMessage prunableMessage = prunableMessageHome.getPrunableMessage(transaction.getFullHash());
                if (transaction.getPrunablePlainMessage() != null) {
                    if (prunableMessage == null || prunableMessage.getMessage() == null) {
                        message = "MISSING";
                    } else {
                        message = quote(Convert.toString(prunableMessage.getMessage(), prunableMessage.messageIsText()));
                    }
                }
                if (transaction.getPrunableEncryptedMessage() != null) {
                    if (prunableMessage == null || prunableMessage.getEncryptedData() == null) {
                        encryptedMessage = "MISSING";
                    } else if (privateKey == null) {
                        encryptedMessage = "ENCRYPTED";
                    } else {
                        byte[] senderPublicKey = Account.getPublicKey(transaction.getSenderId());
                        byte[] recipientPublicKey = Account.getPublicKey(transaction.getRecipientId());
                        byte[] publicKey = Arrays.equals(senderPublicKey, myPublicKey) ? recipientPublicKey : senderPublicKey;
                        if (publicKey != null) {
                            try {
                                encryptedMessage = quote(Convert.toString(
                                        Account.decryptFrom(privateKey, publicKey, prunableMessage.getEncryptedData(), prunableMessage.isCompressed()),
                                        prunableMessage.encryptedMessageIsText()));
                            } catch (RuntimeException e) {
                                encryptedMessage = "UNDECRYPTABLE";
                            }
                        } else {
                            encryptedMessage = "UNDECRYPTABLE";
                        }
                    }
                }
            }
            buf.append(Convert.nullToEmpty(message)).append(SEPARATOR);
            buf.append(Convert.nullToEmpty(encryptedMessage)).append(SEPARATOR);
            buf.append(Convert.nullToEmpty(encryptToSelfMessage)).append(SEPARATOR);
        } else {
            for (int i = 0; i < 7; i++) {
                buf.append(SEPARATOR);
            }
        }
        return buf.toString();
    }

    private String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private byte[] getPrivateKey(String rsAccount) {
        byte[] privateKey = privateKeys.get(rsAccount);
        if (privateKey == null) {
            String secretPhrase = Nxt.getStringProperty("nxt.secretPhrase." + rsAccount, null, true);
            if (secretPhrase != null) {
                privateKey = Crypto.getPrivateKey(secretPhrase);
            } else {
                privateKey = Convert.parseHexString(Nxt.getStringProperty("nxt.privateKey." + rsAccount, null, true));
            }
            if (privateKey == null) {
                privateKey = Convert.EMPTY_BYTE;
            }
            privateKeys.put(rsAccount, privateKey);
        }
        return Convert.emptyToNull(privateKey);
    }

    private byte[] getPublicKey(byte[] privateKey) {
        return privateKey == null ? null : publicKeys.computeIfAbsent(privateKey, Crypto::getPublicKey);
    }
}
