package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.Transaction;

public interface ClientCallback<T> {
	T doWithClient(DatabaseClient client, Transaction transaction);
}
