package nxt.addons.taxreport;

import nxt.Nxt;
import nxt.Tester;
import nxt.ae.AssetIssuanceAttachment;
import nxt.blockchain.Attachment;
import nxt.blockchain.Transaction;
import nxt.blockchain.TransactionProcessor;
import nxt.blockchain.TransactionType;
import nxt.util.Listener;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TransactionsRule extends ExternalResource {
    private final List<Transaction> transactions = synchronizedList(new ArrayList<>());
    private final Listener<List<? extends Transaction>> listener = transactions::addAll;

    @Override
    protected void before() {
        Nxt.getTransactionProcessor().addListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    @Override
    protected void after() {
        transactions.clear();
        Nxt.getTransactionProcessor().removeListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    public void printCollectedTransactions() {
        for (Transaction transaction : transactions) {
            System.out.println(transactionToString(transaction));
        }
    }

    private static String transactionToString(Transaction transaction) {
        return transaction.getType() + ": " + Long.toUnsignedString(transaction.getId());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    @SafeVarargs
    final Transaction findOnly(Predicate<Transaction>... andPredicates) {
        final List<Transaction> found = findAtLeastOne(andPredicates);
        final List<String> foundIds = found.stream().map(TransactionsRule::transactionToString).collect(toList());
        assertEquals("Actually found: " + foundIds, 1, found.size());
        return found.get(0);
    }

    @SafeVarargs
    final Transaction findFirst(Predicate<Transaction>... andPredicates) {
        return findAtLeastOne(andPredicates).get(0);
    }

    @SafeVarargs
    final Transaction findLast(Predicate<Transaction>... andPredicates) {
        final List<Transaction> found = findAtLeastOne(andPredicates);
        return found.get(found.size() - 1);
    }

    @SafeVarargs
    final List<Transaction> findAtLeastOne(Predicate<Transaction>... andPredicates) {
        final List<Transaction> found = transactions.stream().filter(and(andPredicates)).collect(toList());
        if (found.size() == 0) {
            final List<String> allIds = transactions.stream().map(TransactionsRule::transactionToString).collect(toList());
            fail("Found none, all: " + allIds);
        }
        return found;
    }

    private Predicate<Transaction> and(Predicate<Transaction>[] andPredicates) {
        return Stream.of(andPredicates).reduce(Predicate::and).get();
    }

    public static <T extends Attachment.AbstractAttachment> Predicate<Transaction> attach(Class<T> clazz, Predicate<T> predicate) {
        return t -> {
            final Attachment attachment = t.getAttachment();
            return clazz.isAssignableFrom(attachment.getClass()) && predicate.test(clazz.cast(attachment));
        };
    }

    public static Predicate<Transaction> type(TransactionType type) {
        return t -> t.getType() == type;
    }

    public static Predicate<Transaction> assetName(String assetName) {
        return attach(AssetIssuanceAttachment.class, a -> assetName.equals(a.getName()));
    }

    public static Predicate<Transaction> recipient(Tester tester) {
        return t -> t.getRecipientId() == tester.getId();
    }

    public static Predicate<Transaction> sender(Tester tester) {
        return t -> t.getSenderId() == tester.getId();
    }
}
