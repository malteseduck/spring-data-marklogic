package org.springframework.data.marklogic.repository.custom;

import org.springframework.data.marklogic.repository.MarkLogicRepository;

public interface PersonRepository extends MarkLogicRepository<Person, String>, PersonRepositoryCustom {
}
