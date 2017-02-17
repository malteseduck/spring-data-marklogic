package org.springframework.data.marklogic.core;

import com.marklogic.client.query.QueryManager;

public interface QueryCallback<T> {
    T doInMarkLogic(QueryManager mgr);
}
