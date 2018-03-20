package io.github.malteseduck.springframework.data.marklogic.repository.custom;

import io.github.malteseduck.springframework.data.marklogic.repository.MarkLogicRepository;

public interface PersonRepository extends MarkLogicRepository<Person, String>, PersonRepositoryCustom {
}
