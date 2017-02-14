package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import javax.net.ssl.SSLContext;

import static com.marklogic.client.DatabaseClientFactory.*;

public class MarkLogicClientFactoryBean extends AbstractFactoryBean<DatabaseClient> implements PersistenceExceptionTranslator {

    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private Authentication type;
    private SSLContext context;
    private SSLHostnameVerifier verifier = SSLHostnameVerifier.COMMON;

    @Override
    public Class<?> getObjectType() {
        return DatabaseClient.class;
    }

    @Override
    protected DatabaseClient createInstance() throws Exception {
        return newClient(host, port, database, user, password, type, context, verifier);
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException e) {
        return null;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setType(Authentication type) {
        this.type = type;
    }

    public void setContext(SSLContext context) {
        this.context = context;
    }

    public void setVerifier(SSLHostnameVerifier verifier) {
        this.verifier = verifier;
    }
}
