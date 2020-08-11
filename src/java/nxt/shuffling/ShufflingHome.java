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

package nxt.shuffling;

import nxt.Constants;
import nxt.Nxt;
import nxt.account.Account;
import nxt.account.AccountLedger;
import nxt.account.BalanceHome;
import nxt.account.HoldingType;
import nxt.blockchain.Block;
import nxt.blockchain.BlockchainProcessor;
import nxt.blockchain.ChildBlockFxtTransaction;
import nxt.blockchain.ChildBlockFxtTransactionType;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtTransaction;
import nxt.blockchain.Transaction;
import nxt.crypto.AnonymouslyEncryptedData;
import nxt.crypto.Crypto;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShufflingHome {

    public enum Event {
        SHUFFLING_CREATED, SHUFFLING_PROCESSING_ASSIGNED, SHUFFLING_PROCESSING_FINISHED, SHUFFLING_BLAME_STARTED, SHUFFLING_CANCELLED, SHUFFLING_DONE
    }

    private static final boolean deleteFinished = Nxt.getBooleanProperty("nxt.deleteFinishedShufflings");

    public static ShufflingHome forChain(ChildChain childChain) {
        if (childChain.getShufflingHome() != null) {
            throw new IllegalStateException("already set");
        }
        return new ShufflingHome(childChain);
    }

    private final static Listeners<Shuffling, Event> listeners = new Listeners<>();

    public static boolean addListener(Listener<Shuffling> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Shuffling> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    private final DbKey.HashKeyFactory<Shuffling> shufflingDbKeyFactory;
    private final VersionedEntityDbTable<Shuffling> shufflingTable;
    private final ChildChain childChain;

    private ShufflingHome(ChildChain childChain) {
        this.childChain = childChain;
        this.shufflingDbKeyFactory = new DbKey.HashKeyFactory<Shuffling>("full_hash", "id") {
            @Override
            public DbKey newKey(Shuffling shuffling) {
                return shuffling.dbKey;
            }
        };
        this.shufflingTable = new VersionedEntityDbTable<Shuffling>(childChain.getSchemaTable("shuffling"), shufflingDbKeyFactory) {
            @Override
            protected Shuffling load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
                return new Shuffling(rs, dbKey);
            }
            @Override
            protected void save(Connection con, Shuffling shuffling) throws SQLException {
                shuffling.save(con);
            }
        };
        Nxt.getBlockchainProcessor().addListener(block -> {
            List<Shuffling> shufflings = new ArrayList<>();
            try (DbIterator<Shuffling> iterator = getActiveShufflings(0, -1)) {
                for (Shuffling shuffling : iterator) {
                    if (!shuffling.isFull(block)) {
                        shufflings.add(shuffling);
                    }
                }
            }
            shufflings.forEach(shuffling -> {
                if (--shuffling.blocksRemaining <= 0) {
                    shuffling.cancel(block);
                } else {
                    shufflingTable.insert(shuffling);
                }
            });
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    public int getCount() {
        return shufflingTable.getCount();
    }

    public int getActiveCount() {
        return shufflingTable.getCount(new DbClause.NotNullClause("blocks_remaining"));
    }

    public DbIterator<Shuffling> getAll(int from, int to) {
        return shufflingTable.getAll(from, to, " ORDER BY blocks_remaining NULLS LAST, height DESC ");
    }

    public DbIterator<Shuffling> getActiveShufflings(int from, int to) {
        return shufflingTable.getManyBy(new DbClause.NotNullClause("blocks_remaining"), from, to, " ORDER BY blocks_remaining, height DESC ");
    }

    public DbIterator<Shuffling> getFinishedShufflings(int from, int to) {
        return shufflingTable.getManyBy(new DbClause.NullClause("blocks_remaining"), from, to, " ORDER BY height DESC ");
    }

    public Shuffling getShuffling(byte[] fullHash) {
        return shufflingTable.get(shufflingDbKeyFactory.newKey(fullHash));
    }

    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        DbClause clause = holdingId != 0 ? new DbClause.LongClause("holding_id", holdingId) : new DbClause.NullClause("holding_id");
        if (!includeFinished) {
            clause = clause.and(new DbClause.NotNullClause("blocks_remaining"));
        }
        return shufflingTable.getCount(clause);
    }

    public DbIterator<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to) {
        DbClause clause = holdingId != 0 ? new DbClause.LongClause("holding_id", holdingId) : new DbClause.NullClause("holding_id");
        if (!includeFinished) {
            clause = clause.and(new DbClause.NotNullClause("blocks_remaining"));
        }
        if (stage != null) {
            clause = clause.and(new DbClause.ByteClause("stage", stage.getCode()));
        }
        return shufflingTable.getManyBy(clause, from, to, " ORDER BY blocks_remaining NULLS LAST, height DESC ");
    }

    public DbIterator<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        Connection con = null;
        try {
            con = shufflingTable.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT shuffling.* FROM shuffling, shuffling_participant WHERE "
                    + "shuffling_participant.account_id = ? AND shuffling.id = shuffling_participant.shuffling_id "
                    + "AND shuffling.full_hash = shuffling_participant.shuffling_full_hash "
                    + (includeFinished ? "" : "AND shuffling.blocks_remaining IS NOT NULL ")
                    + "AND shuffling.latest = TRUE AND shuffling_participant.latest = TRUE ORDER BY blocks_remaining NULLS LAST, height DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return shufflingTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return shufflingTable.getManyBy(new DbClause.LongClause("assignee_account_id", assigneeAccountId)
                        .and(new DbClause.ByteClause("stage", ShufflingStage.PROCESSING.getCode())), from, to,
                " ORDER BY blocks_remaining NULLS LAST, height DESC ");
    }

    void addShuffling(Transaction transaction, ShufflingCreationAttachment attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        shufflingTable.insert(shuffling);
        childChain.getShufflingParticipantHome().addParticipant(shuffling, transaction.getSenderId(), 0);
        listeners.notify(shuffling, Event.SHUFFLING_CREATED);
    }

    static byte[] getParticipantsHash(Iterable<ShufflingParticipantHome.ShufflingParticipant> participants) {
        MessageDigest digest = Crypto.sha256();
        participants.forEach(participant -> digest.update(Convert.toBytes(participant.getAccountId())));
        return digest.digest();
    }

    public final class Shuffling {

        private final ShufflingParticipantHome shufflingParticipantHome = childChain.getShufflingParticipantHome();
        private final byte[] hash;
        private final long id;
        private final DbKey dbKey;
        private final long holdingId;
        private final HoldingType holdingType;
        private final long issuerId;
        private final long amount;
        private final byte participantCount;
        private short blocksRemaining;
        private byte registrantCount;
        private ShufflingStage stage;
        private long assigneeAccountId;
        private byte[][] recipientPublicKeys;

        private Shuffling(Transaction transaction, ShufflingCreationAttachment attachment) {
            this.id = transaction.getId();
            this.hash = transaction.getFullHash();
            this.dbKey = shufflingDbKeyFactory.newKey(this.hash, this.id);
            this.holdingId = attachment.getHoldingId();
            this.holdingType = attachment.getHoldingType();
            this.issuerId = transaction.getSenderId();
            this.amount = attachment.getAmount();
            this.participantCount = attachment.getParticipantCount();
            this.blocksRemaining = attachment.getRegistrationPeriod();
            this.stage = ShufflingStage.REGISTRATION;
            this.assigneeAccountId = issuerId;
            this.recipientPublicKeys = Convert.EMPTY_BYTES;
            this.registrantCount = 1;
        }

        private Shuffling(ResultSet rs, DbKey dbKey) throws SQLException {
            this.id = rs.getLong("id");
            this.hash = rs.getBytes("full_hash");
            this.dbKey = dbKey;
            this.holdingId = rs.getLong("holding_id");
            this.holdingType = HoldingType.get(rs.getByte("holding_type"));
            this.issuerId = rs.getLong("issuer_id");
            this.amount = rs.getLong("amount");
            this.participantCount = rs.getByte("participant_count");
            this.blocksRemaining = rs.getShort("blocks_remaining");
            this.stage = ShufflingStage.get(rs.getByte("stage"));
            this.assigneeAccountId = rs.getLong("assignee_account_id");
            this.recipientPublicKeys = DbUtils.getArray(rs, "recipient_public_keys", byte[][].class, Convert.EMPTY_BYTES);
            this.registrantCount = rs.getByte("registrant_count");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling (id, full_hash, holding_id, holding_type, "
                    + "issuer_id, amount, participant_count, blocks_remaining, stage, assignee_account_id, "
                    + "recipient_public_keys, registrant_count, height, latest) "
                    + "KEY (id, full_hash, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.id);
                pstmt.setBytes(++i, this.hash);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.holdingId);
                pstmt.setByte(++i, this.holdingType.getCode());
                pstmt.setLong(++i, this.issuerId);
                pstmt.setLong(++i, this.amount);
                pstmt.setByte(++i, this.participantCount);
                DbUtils.setShortZeroToNull(pstmt, ++i, this.blocksRemaining);
                pstmt.setByte(++i, this.getStage().getCode());
                DbUtils.setLongZeroToNull(pstmt, ++i, this.assigneeAccountId);
                DbUtils.setArrayEmptyToNull(pstmt, ++i, this.recipientPublicKeys);
                pstmt.setByte(++i, this.registrantCount);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public ShufflingParticipantHome getShufflingParticipantHome() {
            return shufflingParticipantHome;
        }

        public final ChildChain getChildChain() {
            return ShufflingHome.this.childChain;
        }

        public long getId() {
            return id;
        }

        public byte[] getFullHash() {
            return hash;
        }

        public long getHoldingId() {
            return holdingId;
        }

        public HoldingType getHoldingType() {
            return holdingType;
        }

        public long getIssuerId() {
            return issuerId;
        }

        public long getAmount() {
            return amount;
        }

        public byte getParticipantCount() {
            return participantCount;
        }

        public byte getRegistrantCount() {
            return registrantCount;
        }

        public short getBlocksRemaining() {
            return blocksRemaining;
        }

        public ShufflingStage getStage() {
            return stage;
        }

        // caller must update database
        private void setStage(ShufflingStage stage, long assigneeAccountId, short blocksRemaining) {
            if (!this.stage.canBecome(stage)) {
                throw new IllegalStateException(String.format("Shuffling in stage %s cannot go to stage %s", this.stage, stage));
            }
            if ((stage == ShufflingStage.VERIFICATION || stage == ShufflingStage.DONE) && assigneeAccountId != 0) {
                throw new IllegalArgumentException(String.format("Invalid assigneeAccountId %s for stage %s", Convert.rsAccount(assigneeAccountId), stage));
            }
            if ((stage == ShufflingStage.REGISTRATION || stage == ShufflingStage.PROCESSING || stage == ShufflingStage.BLAME) && assigneeAccountId == 0) {
                throw new IllegalArgumentException(String.format("In stage %s assigneeAccountId cannot be 0", stage));
            }
            if ((stage == ShufflingStage.DONE || stage == ShufflingStage.CANCELLED) && blocksRemaining != 0) {
                throw new IllegalArgumentException(String.format("For stage %s remaining blocks cannot be %s", stage, blocksRemaining));
            }
            this.stage = stage;
            this.assigneeAccountId = assigneeAccountId;
            this.blocksRemaining = blocksRemaining;
            Logger.logDebugMessage("Shuffling %s entered stage %s, assignee %s, remaining blocks %s",
                    Long.toUnsignedString(id), this.stage, Convert.rsAccount(this.assigneeAccountId), this.blocksRemaining);
        }

        /*
        * Meaning of assigneeAccountId in each shuffling stage:
        *  REGISTRATION: last currently registered participant
        *  PROCESSING: next participant in turn to submit processing data
        *  VERIFICATION: 0, not assigned to anyone
        *  BLAME: the participant who initiated the blame phase
        *  CANCELLED: the participant who got blamed for the shuffling failure, if any
        *  DONE: 0, not assigned to anyone
        */
        public long getAssigneeAccountId() {
            return assigneeAccountId;
        }

        public byte[][] getRecipientPublicKeys() {
            return recipientPublicKeys;
        }

        public ShufflingParticipantHome.ShufflingParticipant getParticipant(long accountId) {
            return shufflingParticipantHome.getParticipant(this.hash, accountId);
        }

        public ShufflingParticipantHome.ShufflingParticipant getLastParticipant() {
            return shufflingParticipantHome.getLastParticipant(this.hash);
        }

        public byte[] getStateHash() {
            return stage.getHash(this);
        }

        public ShufflingAttachment process(final long accountId, byte[] privateKey, final byte[] recipientPublicKey) {
            byte[][] data = Convert.EMPTY_BYTES;
            byte[] shufflingStateHash = null;
            int participantIndex = 0;
            List<ShufflingParticipantHome.ShufflingParticipant> shufflingParticipants = new ArrayList<>();
            Nxt.getBlockchain().readLock();
            // Read the participant list for the shuffling
            try (DbIterator<ShufflingParticipantHome.ShufflingParticipant> participants = this.shufflingParticipantHome.getParticipants(this.hash)) {
                for (ShufflingParticipantHome.ShufflingParticipant participant : participants) {
                    shufflingParticipants.add(participant);
                    if (participant.getNextAccountId() == accountId) {
                        data = participant.getData();
                        shufflingStateHash = participant.getDataTransactionFullHash();
                        participantIndex = shufflingParticipants.size();
                    }
                }
                if (shufflingStateHash == null) {
                    shufflingStateHash = getParticipantsHash(shufflingParticipants);
                }
            } finally {
                Nxt.getBlockchain().readUnlock();
            }
            boolean isLast = participantIndex == participantCount - 1;
            // decrypt the tokens bundled in the current data
            List<byte[]> outputDataList = new ArrayList<>();
            for (byte[] bytes : data) {
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                try {
                    byte[] decrypted = encryptedData.decrypt(privateKey);
                    outputDataList.add(decrypted);
                } catch (Exception e) {
                    Logger.logMessage("Decryption failed", e);
                    return isLast ? new ShufflingRecipientsAttachment(this.hash, Convert.EMPTY_BYTES, shufflingStateHash)
                            : new ShufflingProcessingAttachment(this.hash, Convert.EMPTY_BYTES, shufflingStateHash);
                }
            }
            // Calculate the token for the current sender by iteratively encrypting it using the public key of all the participants
            // which did not perform shuffle processing yet
            byte[] bytesToEncrypt = recipientPublicKey;
            for (int i = shufflingParticipants.size() - 1; i > participantIndex; i--) {
                ShufflingParticipantHome.ShufflingParticipant participant = shufflingParticipants.get(i);
                byte[] participantPublicKey = Account.getPublicKey(participant.getAccountId());
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.encrypt(bytesToEncrypt, privateKey, participantPublicKey, this.hash);
                bytesToEncrypt = encryptedData.getBytes();
            }
            outputDataList.add(bytesToEncrypt);
            // Shuffle the tokens and save the shuffled tokens as the participant data
            outputDataList.sort(Convert.byteArrayComparator);
            if (isLast) {
                Set<Long> recipientAccounts = new HashSet<>(participantCount);
                for (byte[] publicKey : outputDataList) {
                    if (!Crypto.isCanonicalPublicKey(publicKey) || !recipientAccounts.add(Account.getId(publicKey))) {
                        // duplicate or invalid recipient public key
                        Logger.logDebugMessage("Invalid recipient public key " + Convert.toHexString(publicKey));
                        return new ShufflingRecipientsAttachment(this.hash, Convert.EMPTY_BYTES, shufflingStateHash);
                    }
                }
                // last participant prepares ShufflingRecipients transaction instead of ShufflingProcessing
                return new ShufflingRecipientsAttachment(this.hash, outputDataList.toArray(new byte[outputDataList.size()][]),
                        shufflingStateHash);
            } else {
                byte[] previous = null;
                for (byte[] decrypted : outputDataList) {
                    if (previous != null && Arrays.equals(decrypted, previous)) {
                        Logger.logDebugMessage("Duplicate decrypted data");
                        return new ShufflingProcessingAttachment(this.hash, Convert.EMPTY_BYTES, shufflingStateHash);
                    }
                    if (decrypted.length != 32 + 64 * (participantCount - participantIndex - 1)) {
                        Logger.logDebugMessage("Invalid encrypted data length in process " + decrypted.length);
                        return new ShufflingProcessingAttachment(this.hash, Convert.EMPTY_BYTES, shufflingStateHash);
                    }
                    previous = decrypted;
                }
                return new ShufflingProcessingAttachment(this.hash, outputDataList.toArray(new byte[outputDataList.size()][]),
                        shufflingStateHash);
            }
        }

        public ShufflingCancellationAttachment revealKeySeeds(byte[] privateKey, long cancellingAccountId, byte[] shufflingStateHash) {
            Nxt.getBlockchain().readLock();
            try (DbIterator<ShufflingParticipantHome.ShufflingParticipant> participants = shufflingParticipantHome.getParticipants(this.hash)) {
                if (cancellingAccountId != this.assigneeAccountId) {
                    throw new RuntimeException(String.format("Current shuffling cancellingAccountId %s does not match %s",
                            Convert.rsAccount(this.assigneeAccountId), Convert.rsAccount(cancellingAccountId)));
                }
                if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, getStateHash())) {
                    throw new RuntimeException("Current shuffling state hash does not match");
                }
                long accountId = Account.getId(Crypto.getPublicKey(privateKey));
                byte[][] data = null;
                while (participants.hasNext()) {
                    ShufflingParticipantHome.ShufflingParticipant participant = participants.next();
                    if (participant.getAccountId() == accountId) {
                        data = participant.getData();
                        break;
                    }
                }
                if (!participants.hasNext()) {
                    throw new RuntimeException("Last participant cannot have keySeeds to reveal");
                }
                if (data == null) {
                    throw new RuntimeException("Account " + Convert.rsAccount(accountId) + " has not submitted data");
                }
                final byte[] nonce = this.hash;
                final List<byte[]> keySeeds = new ArrayList<>();
                byte[] nextParticipantPublicKey = Account.getPublicKey(participants.next().getAccountId());
                byte[] keySeed = Crypto.getKeySeed(privateKey, nextParticipantPublicKey, nonce);
                keySeeds.add(keySeed);
                byte[] publicKey = Crypto.getPublicKey(keySeed);
                byte[] decryptedBytes = null;
                // find the data that we encrypted
                for (byte[] bytes : data) {
                    AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                    if (Arrays.equals(encryptedData.getPublicKey(), publicKey)) {
                        try {
                            decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                            break;
                        } catch (Exception ignore) {
                        }
                    }
                }
                if (decryptedBytes == null) {
                    throw new RuntimeException("None of the encrypted data could be decrypted");
                }
                // decrypt all iteratively, adding the key seeds to the result
                while (participants.hasNext()) {
                    nextParticipantPublicKey = Account.getPublicKey(participants.next().getAccountId());
                    keySeed = Crypto.getKeySeed(privateKey, nextParticipantPublicKey, nonce);
                    keySeeds.add(keySeed);
                    AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(decryptedBytes);
                    decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                }
                return new ShufflingCancellationAttachment(this.hash, data, keySeeds.toArray(new byte[keySeeds.size()][]),
                        shufflingStateHash, cancellingAccountId);
            } finally {
                Nxt.getBlockchain().readUnlock();
            }
        }

        void addParticipant(long participantId) {
            // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
            // to the new participant
            ShufflingParticipantHome.ShufflingParticipant lastParticipant = shufflingParticipantHome.getParticipant(this.hash, this.assigneeAccountId);
            lastParticipant.setNextAccountId(participantId);
            shufflingParticipantHome.addParticipant(this, participantId, this.registrantCount);
            this.registrantCount += 1;
            // Check if participant registration is complete and if so update the shuffling
            if (this.registrantCount == this.participantCount) {
                setStage(ShufflingStage.PROCESSING, this.issuerId, Constants.SHUFFLING_PROCESSING_DEADLINE);
            } else {
                this.assigneeAccountId = participantId;
            }
            shufflingTable.insert(this);
            if (stage == ShufflingStage.PROCESSING) {
                listeners.notify(this, Event.SHUFFLING_PROCESSING_ASSIGNED);
            }
        }

        void updateParticipantData(Transaction transaction, ShufflingProcessingAttachment attachment) {
            long participantId = transaction.getSenderId();
            byte[][] data = attachment.getData();
            ShufflingParticipantHome.ShufflingParticipant participant = shufflingParticipantHome.getParticipant(this.hash, participantId);
            participant.setData(data, transaction.getTimestamp());
            participant.setProcessed(transaction.getFullHash());
            if (data != null && data.length == 0) {
                // couldn't decrypt all data from previous participants
                cancelBy(participant);
                return;
            }
            this.assigneeAccountId = participant.getNextAccountId();
            this.blocksRemaining = Constants.SHUFFLING_PROCESSING_DEADLINE;
            shufflingTable.insert(this);
            listeners.notify(this, Event.SHUFFLING_PROCESSING_ASSIGNED);
        }

        void updateRecipients(Transaction transaction, ShufflingRecipientsAttachment attachment) {
            long participantId = transaction.getSenderId();
            this.recipientPublicKeys = attachment.getRecipientPublicKeys();
            ShufflingParticipantHome.ShufflingParticipant participant = shufflingParticipantHome.getParticipant(this.hash, participantId);
            participant.setProcessed(transaction.getFullHash());
            if (recipientPublicKeys.length == 0) {
                // couldn't decrypt all data from previous participants
                cancelBy(participant);
                return;
            }
            participant.verify();
            // last participant announces all valid recipient public keys
            for (byte[] recipientPublicKey : recipientPublicKeys) {
                long recipientId = Account.getId(recipientPublicKey);
                if (Account.setOrVerify(recipientId, recipientPublicKey)) {
                    Account.addOrGetAccount(recipientId).apply(recipientPublicKey);
                }
            }
            setStage(ShufflingStage.VERIFICATION, 0, (short) (Constants.SHUFFLING_PROCESSING_DEADLINE + participantCount));
            shufflingTable.insert(this);
            listeners.notify(this, Event.SHUFFLING_PROCESSING_FINISHED);
        }

        void verify(long accountId) {
            shufflingParticipantHome.getParticipant(this.hash, accountId).verify();
            if (shufflingParticipantHome.getVerifiedCount(this) == participantCount) {
                distribute();
            }
        }

        void cancelBy(ShufflingParticipantHome.ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
            participant.cancel(blameData, keySeeds);
            boolean startingBlame = this.stage != ShufflingStage.BLAME;
            if (startingBlame) {
                setStage(ShufflingStage.BLAME, participant.getAccountId(), (short) (Constants.SHUFFLING_PROCESSING_DEADLINE + participantCount));
            }
            shufflingTable.insert(this);
            if (startingBlame) {
                listeners.notify(this, Event.SHUFFLING_BLAME_STARTED);
            }
        }

        private void cancelBy(ShufflingParticipantHome.ShufflingParticipant participant) {
            cancelBy(participant, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
        }

        private void distribute() {
            if (recipientPublicKeys.length != participantCount) {
                cancelBy(getLastParticipant());
                return;
            }
            for (byte[] recipientPublicKey : recipientPublicKeys) {
                byte[] publicKey = Account.getPublicKey(Account.getId(recipientPublicKey));
                if (publicKey != null && !Arrays.equals(publicKey, recipientPublicKey)) {
                    // distribution not possible, do a cancellation on behalf of last participant instead
                    cancelBy(getLastParticipant());
                    return;
                }
            }
            AccountLedger.LedgerEvent event = AccountLedger.LedgerEvent.SHUFFLING_DISTRIBUTION;
            AccountLedger.LedgerEventId eventId = AccountLedger.newEventId(this.id, this.getFullHash(), childChain);
            try (DbIterator<ShufflingParticipantHome.ShufflingParticipant> participants = shufflingParticipantHome.getParticipants(this.hash)) {
                for (ShufflingParticipantHome.ShufflingParticipant participant : participants) {
                    Account participantAccount = Account.getAccount(participant.getAccountId());
                    holdingType.addToBalance(participantAccount, event, eventId, this.holdingId, -amount);
                    if (holdingType != HoldingType.COIN) {
                        participantAccount.addToBalance(childChain, event, eventId, -childChain.SHUFFLING_DEPOSIT_NQT);
                    }
                }
            }
            for (byte[] recipientPublicKey : recipientPublicKeys) {
                long recipientId = Account.getId(recipientPublicKey);
                Account recipientAccount = Account.addOrGetAccount(recipientId);
                recipientAccount.apply(recipientPublicKey);
                holdingType.addToBalanceAndUnconfirmedBalance(recipientAccount, event, eventId, this.holdingId, amount);
                if (holdingType != HoldingType.COIN) {
                    recipientAccount.addToBalanceAndUnconfirmedBalance(childChain, event, eventId, childChain.SHUFFLING_DEPOSIT_NQT);
                }
            }
            setStage(ShufflingStage.DONE, 0, (short) 0);
            shufflingTable.insert(this);
            listeners.notify(this, Event.SHUFFLING_DONE);
            if (deleteFinished) {
                delete();
            }
            Logger.logDebugMessage("Shuffling %s was distributed", Long.toUnsignedString(id));
        }

        private void cancel(Block block) {
            long blamedAccountId = blame();
            try (DbIterator<ShufflingParticipantHome.ShufflingParticipant> participants = shufflingParticipantHome.getParticipants(this.hash)) {
                AccountLedger.LedgerEvent event = AccountLedger.LedgerEvent.SHUFFLING_CANCELLATION;
                AccountLedger.LedgerEventId eventId = AccountLedger.newEventId(this.id, this.getFullHash(), childChain);
                for (ShufflingParticipantHome.ShufflingParticipant participant : participants) {
                    Account participantAccount = Account.getAccount(participant.getAccountId());
                    holdingType.addToUnconfirmedBalance(participantAccount, event, eventId, this.holdingId, this.amount);
                    if (participantAccount.getId() != blamedAccountId) {
                        if (holdingType != HoldingType.COIN) {
                            participantAccount.addToUnconfirmedBalance(childChain, event, eventId, childChain.SHUFFLING_DEPOSIT_NQT);
                        }
                    } else {
                        if (holdingType == HoldingType.COIN) {
                            participantAccount.addToUnconfirmedBalance(childChain, event, eventId, -childChain.SHUFFLING_DEPOSIT_NQT);
                        }
                        participantAccount.addToBalance(childChain, event, eventId, -childChain.SHUFFLING_DEPOSIT_NQT);
                    }
                }
            }
            if (blamedAccountId != 0) {
                AccountLedger.LedgerEventId eventId = AccountLedger.newEventId(block);
                // as a penalty the deposit goes to the generators of the finish block and previous 3 blocks
                long fee = childChain.SHUFFLING_DEPOSIT_NQT / 4;
                for (int i = 0; i < 3; i++) {
                    BalanceHome.Balance previousGeneratorBalance = childChain.getBalanceHome().getBalance(Nxt.getBlockchain().getBlockAtHeight(block.getHeight() - i - 1).getGeneratorId());
                    previousGeneratorBalance.addToBalanceAndUnconfirmedBalance(AccountLedger.LedgerEvent.BLOCK_GENERATED, eventId, fee);
                    Logger.logDebugMessage("Shuffling penalty %f %s awarded to forger at height %d", ((double) fee) / childChain.ONE_COIN,
                            childChain.getName(), block.getHeight() - i - 1);
                }
                fee = childChain.SHUFFLING_DEPOSIT_NQT - 3 * fee;
                BalanceHome.Balance blockGeneratorBalance = childChain.getBalanceHome().getBalance(block.getGeneratorId());
                blockGeneratorBalance.addToBalanceAndUnconfirmedBalance(AccountLedger.LedgerEvent.BLOCK_GENERATED, eventId, fee);
                Logger.logDebugMessage("Shuffling penalty %f %s awarded to forger at height %d", ((double) fee) / childChain.ONE_COIN,
                        childChain.getName(), block.getHeight());
            }
            setStage(ShufflingStage.CANCELLED, blamedAccountId, (short) 0);
            shufflingTable.insert(this);
            listeners.notify(this, Event.SHUFFLING_CANCELLED);
            if (deleteFinished) {
                delete();
            }
            Logger.logDebugMessage("Shuffling %s was cancelled, blaming account %s", Long.toUnsignedString(id), Convert.rsAccount(blamedAccountId));
        }

        private long blame() {
            // if registration never completed, no one is to blame
            if (stage == ShufflingStage.REGISTRATION) {
                Logger.logDebugMessage("Registration never completed for shuffling %s", Long.toUnsignedString(id));
                return 0;
            }
            // if no one submitted cancellation, blame the first one that did not submit processing data
            if (stage == ShufflingStage.PROCESSING) {
                Logger.logDebugMessage("Participant %s did not submit processing", Convert.rsAccount(assigneeAccountId));
                return assigneeAccountId;
            }
            List<ShufflingParticipantHome.ShufflingParticipant> participants = new ArrayList<>();
            try (DbIterator<ShufflingParticipantHome.ShufflingParticipant> iterator = shufflingParticipantHome.getParticipants(this.hash)) {
                while (iterator.hasNext()) {
                    participants.add(iterator.next());
                }
            }
            if (stage == ShufflingStage.VERIFICATION) {
                // if verification started, blame the first one who did not submit verification
                for (ShufflingParticipantHome.ShufflingParticipant participant : participants) {
                    if (participant.getState() != ShufflingParticipantHome.State.VERIFIED) {
                        Logger.logDebugMessage("Participant %s did not submit verification", Convert.rsAccount(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                }
                throw new RuntimeException("All participants submitted data and verifications, blame phase should not have been entered");
            }
            Set<Long> recipientAccounts = new HashSet<>(participantCount);
            // start from issuer and verify all data up, skipping last participant
            for (int i = 0; i < participantCount - 1; i++) {
                ShufflingParticipantHome.ShufflingParticipant participant = participants.get(i);
                byte[][] keySeeds = participant.getKeySeeds();
                // if participant couldn't submit key seeds because he also couldn't decrypt some of the previous data, this should have been caught before
                if (keySeeds.length == 0) {
                    Logger.logDebugMessage("Participant %s did not reveal keys", Convert.rsAccount(participant.getAccountId()));
                    return participant.getAccountId();
                }
                byte[] publicKey = Crypto.getPublicKey(keySeeds[0]);
                AnonymouslyEncryptedData encryptedData = null;
                for (byte[] bytes : participant.getBlameData()) {
                    encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                    if (Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                        // found the data that this participant encrypted
                        break;
                    }
                }
                if (encryptedData == null || !Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                    // participant lied about key seeds or data
                    Logger.logDebugMessage("Participant %s did not submit blame data, or revealed invalid keys", Convert.rsAccount(participant.getAccountId()));
                    return participant.getAccountId();
                }
                for (int k = i + 1; k < participantCount; k++) {
                    ShufflingParticipantHome.ShufflingParticipant nextParticipant = participants.get(k);
                    byte[] nextParticipantPublicKey = Account.getPublicKey(nextParticipant.getAccountId());
                    byte[] keySeed = keySeeds[k - i - 1];
                    byte[] participantBytes;
                    try {
                        participantBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                    } catch (Exception e) {
                        // the next participant couldn't decrypt the data either, blame this one
                        Logger.logDebugMessage("Could not decrypt data from participant %s", Convert.rsAccount(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                    boolean isLast = k == participantCount - 1;
                    if (isLast) {
                        // not encrypted data but plaintext recipient public key
                        if (!Crypto.isCanonicalPublicKey(publicKey)) {
                            // not a valid public key
                            Logger.logDebugMessage("Participant %s submitted invalid recipient public key", Convert.rsAccount(participant.getAccountId()));
                            return participant.getAccountId();
                        }
                        // check for collisions and assume they are intentional
                        byte[] currentPublicKey = Account.getPublicKey(Account.getId(participantBytes));
                        if (currentPublicKey != null && !Arrays.equals(currentPublicKey, participantBytes)) {
                            Logger.logDebugMessage("Participant %s submitted colliding recipient public key", Convert.rsAccount(participant.getAccountId()));
                            return participant.getAccountId();
                        }
                        if (!recipientAccounts.add(Account.getId(participantBytes))) {
                            Logger.logDebugMessage("Participant %s submitted duplicate recipient public key", Convert.rsAccount(participant.getAccountId()));
                            return participant.getAccountId();
                        }
                    }
                    if (nextParticipant.getState() == ShufflingParticipantHome.State.CANCELLED && nextParticipant.getBlameData().length == 0) {
                        break;
                    }
                    boolean found = false;
                    for (byte[] bytes : isLast ? recipientPublicKeys : nextParticipant.getBlameData()) {
                        if (Arrays.equals(participantBytes, bytes)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // the next participant did not include this participant's data
                        Logger.logDebugMessage("Participant %s did not include previous data", Convert.rsAccount(nextParticipant.getAccountId()));
                        return nextParticipant.getAccountId();
                    }
                    if (!isLast) {
                        encryptedData = AnonymouslyEncryptedData.readEncryptedData(participantBytes);
                    }
                }
            }
            return assigneeAccountId;
        }

        private void delete() {
            try (DbIterator<ShufflingParticipantHome.ShufflingParticipant> participants = shufflingParticipantHome.getParticipants(this.hash)) {
                for (ShufflingParticipantHome.ShufflingParticipant participant : participants) {
                    participant.delete();
                }
            }
            shufflingTable.delete(this);
        }

        private boolean isFull(Block block) {
            int transactionSize = 4 + 1 + 1 + 1 + 4 + 2 + 32 + 8 + 8 + 8 + 64 + 4 + 8 + 4 + 4 + 32;
            if (stage == ShufflingStage.REGISTRATION) {
                transactionSize += 1 + 32;
            } else { // must use same for PROCESSING/VERIFICATION/BLAME
                transactionSize = 16384; // max observed was 15647 for 30 participants
            }
            ChildBlockFxtTransaction childBlockFxtTransaction = null;
            for (FxtTransaction fxtTransaction : block.getFxtTransactions()) {
                if (fxtTransaction.getType() == ChildBlockFxtTransactionType.INSTANCE && ((ChildBlockFxtTransaction)fxtTransaction).getChildChain() == childChain) {
                    childBlockFxtTransaction = (ChildBlockFxtTransaction)fxtTransaction;
                    break;
                }
            }
            if (childBlockFxtTransaction == null) {
                return block.getFxtTransactions().size() == Constants.MAX_NUMBER_OF_FXT_TRANSACTIONS;
            }
            return childBlockFxtTransaction.getFullSize() + transactionSize > Constants.MAX_CHILDBLOCK_PAYLOAD_LENGTH;
        }
    }

}