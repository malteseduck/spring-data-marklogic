package io.github.malteseduck.springframework.data.marklogic.repository;

import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import io.github.malteseduck.springframework.data.marklogic.core.Person;
import io.github.malteseduck.springframework.data.marklogic.core.PersonToStream;
import io.github.malteseduck.springframework.data.marklogic.core.PersonXml;
import io.github.malteseduck.springframework.data.marklogic.repository.support.MarkLogicRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
