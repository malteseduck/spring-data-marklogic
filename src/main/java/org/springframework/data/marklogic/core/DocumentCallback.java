package org.springframework.data.marklogic.core;

import com.marklogic.client.Transaction;
import com.marklogic.client.document.DocumentManager;

public interface DocumentCallback<T> {
    T doInMarkLogic(DocumentManager mgr, Transaction transaction);
}
