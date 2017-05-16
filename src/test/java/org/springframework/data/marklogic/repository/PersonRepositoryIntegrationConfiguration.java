package org.springframework.data.marklogic.repository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.PersonToStream;
import org.springframework.data.marklogic.core.PersonXml;
import org.springframework.data.marklogic.repository.support.MarkLogicRepositoryFactoryBean;

@Configuration
public class PersonRepositoryIntegrationConfiguration {

    @Bean
    MarkLogicRepositoryFactoryBean repository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean<PersonRepository, Person, String> repository = new MarkLogicRepositoryFactoryBean<>(PersonRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }

    @Bean
    MarkLogicRepositoryFactoryBean xmlRepository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean<PersonXmlRepository, PersonXml, String> repository = new MarkLogicRepositoryFactoryBean<>(PersonXmlRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }

    @Bean
    MarkLogicRepositoryFactoryBean streamRepository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean<PersonStreamRepository, PersonToStream, String> repository = new MarkLogicRepositoryFactoryBean<>(PersonStreamRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }

    @Bean
    MarkLogicRepositoryFactoryBean transRepository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean<PersonTransformingRepository, Person, String> repository = new MarkLogicRepositoryFactoryBean<>(PersonTransformingRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }
}
