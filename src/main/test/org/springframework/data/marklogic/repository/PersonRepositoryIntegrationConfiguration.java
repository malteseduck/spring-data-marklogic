package org.springframework.data.marklogic.repository;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.marklogic.core.MarkLogicClientFactoryBean;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.repository.support.MarkLogicRepositoryFactoryBean;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class PersonRepositoryIntegrationConfiguration {

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

    @Bean
    MarkLogicTemplate template(DatabaseClient client) {
        return new MarkLogicTemplate(client);
    }

    @Bean
    MarkLogicRepositoryFactoryBean repository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean repository = new MarkLogicRepositoryFactoryBean(PersonRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }
}
