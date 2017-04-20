package org.springframework.data.marklogic.repository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.repository.support.MarkLogicRepositoryFactoryBean;

@Configuration
public class PersonRepositoryIntegrationConfiguration {

    @Bean
    MarkLogicRepositoryFactoryBean repository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean repository = new MarkLogicRepositoryFactoryBean(PersonRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }

    @Bean
    MarkLogicRepositoryFactoryBean xmlRepository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean repository = new MarkLogicRepositoryFactoryBean(PersonXmlRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }
}
