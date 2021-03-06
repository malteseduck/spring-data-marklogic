package io.github.malteseduck.springframework.data.marklogic.core;

import com.marklogic.client.Transaction;
import com.marklogic.client.document.DocumentManager;

public interface DocumentCallback<T> {
    T doWithDocumentManager(DocumentManager mgr, Transaction transaction);
}
