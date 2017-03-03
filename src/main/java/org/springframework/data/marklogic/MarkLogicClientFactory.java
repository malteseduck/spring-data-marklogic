package org.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import org.springframework.dao.DataAccessException;

public interface MarkLogicClientFactory {

    /**
     * Creates a default {@link DatabaseClient} instance.
     *
     * @return
     * @throws DataAccessException
     */
    DatabaseClient getClient() throws DataAccessException;

    /**
     * Creates a {@link DatabaseClient} instance to access the data source withOptions the given name.
     *
     * @param sourceName must not be {@literal null} or empty.
     * @return
     * @throws DataAccessException
     */
    DatabaseClient getClient(String sourceName) throws DataAccessException;

    /**
     * Creates a {@link DatabaseClient} instance to access the data source withOptions the given name / account.
     *
     * @param sourceName must not be {@literal null} or empty.
     * @param user name of account
     * @return
     * @throws DataAccessException
     */
    DatabaseClient getClient(String sourceName, String user) throws DataAccessException;

}
