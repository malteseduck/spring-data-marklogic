package io.github.malteseduck.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.Transaction;
import org.springframework.transaction.support.ResourceHolderSupport;

public class TransactionHolder extends ResourceHolderSupport {

    // TODO: The default timeout of an application server is 10 minutes, but if they update the appserver timeout it technically should use that - maybe query it at startup?
    private static final int TIMEOUT_DEFAULT = 10 * 60;

    private boolean newTransaction;
    private Transaction transaction;
    private final DatabaseClient client;
    private String name;

    public TransactionHolder(DatabaseClient client) {
        this.newTransaction = true;
        this.client = client;
        setTimeoutInSeconds(TIMEOUT_DEFAULT);
    }

    public boolean isNewTransaction() {
        return newTransaction;
    }

    public void setNewTransaction(boolean newTransaction) {
        this.newTransaction = newTransaction;
    }

    public void setTransaction(Transaction transaction) {
        setNewTransaction(false);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void begin() {
        if (transaction == null) {
            transaction = client.openTransaction(getName(), getTimeToLiveInSeconds());
        }
    }

    public boolean isTransactionActive() {
        return transaction != null;
    }

    @Override
    public void clear() {
        super.clear();
        transaction = null;
    }
}
