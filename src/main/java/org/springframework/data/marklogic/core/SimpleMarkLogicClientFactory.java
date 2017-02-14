package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.marklogic.MarkLogicClientFactory;

public class SimpleMarkLogicClientFactory implements DisposableBean, MarkLogicClientFactory {

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public DatabaseClient getClient() throws DataAccessException {
        return null;
    }

    @Override
    public DatabaseClient getClient(String sourceName) throws DataAccessException {
        return null;
    }

    @Override
    public DatabaseClient getClient(String sourceName, String user) throws DataAccessException {
        return null;
    }
}
