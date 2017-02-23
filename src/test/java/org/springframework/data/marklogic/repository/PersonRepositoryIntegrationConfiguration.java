package org.springframework.data.marklogic.repository;

import com.marklogic.client.DatabaseClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.marklogic.InfrastructureConfiguration;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.repository.support.MarkLogicRepositoryFactoryBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;

@ContextConfiguration
public class PersonRepositoryIntegrationConfiguration extends InfrastructureConfiguration {

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
