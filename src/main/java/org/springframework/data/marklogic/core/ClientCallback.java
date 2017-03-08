package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.Transaction;

public interface ClientCallback {
    Object doWithClient(DatabaseClient client, Transaction transaction);
}
