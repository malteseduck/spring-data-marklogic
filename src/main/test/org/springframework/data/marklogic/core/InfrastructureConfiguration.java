package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class InfrastructureConfiguration {

    @Bean
    public MarkLogicClientFactoryBean client() {
        MarkLogicClientFactoryBean client = new MarkLogicClientFactoryBean();
        client.setHost("127.0.0.1");
        client.setPort(8000);
        client.setUser("admin");
        client.setPassword("admin");
        client.setType(DatabaseClientFactory.Authentication.DIGEST);
        return client;
    }
}
