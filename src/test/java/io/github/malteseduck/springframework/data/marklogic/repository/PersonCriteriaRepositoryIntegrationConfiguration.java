package io.github.malteseduck.springframework.data.marklogic.repository;

import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import io.github.malteseduck.springframework.data.marklogic.core.Person;
import io.github.malteseduck.springframework.data.marklogic.core.PersonToStream;
import io.github.malteseduck.springframework.data.marklogic.core.PersonXml;
import io.github.malteseduck.springframework.data.marklogic.repository.support.MarkLogicRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonCriteriaRepositoryIntegrationConfiguration {

    @Bean
    MarkLogicRepositoryFactoryBean repository(MarkLogicTemplate template) {
        MarkLogicRepositoryFactoryBean<PersonCriteriaRepository, Person, String> repository = new MarkLogicRepositoryFactoryBean<>(PersonCriteriaRepository.class);
        repository.setMarkLogicOperations(template);
        return repository;
    }

}
