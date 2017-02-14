package org.springframework.data.marklogic.core;

import com.marklogic.client.document.DocumentManager;

public interface MarkLogicCallback<T> {
    T doInMarkLogic(DocumentManager mgr);
}
