package org.springframework.data.marklogic.core;

import com.marklogic.client.document.DocumentManager;

public interface DocumentCallback<T> {
    T doInMarkLogic(DocumentManager mgr);
}
