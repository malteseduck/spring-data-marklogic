package org.springframework.data.marklogic.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.marklogic.core.Person;

public interface PersonTransformingRepository extends MarkLogicRepository<Person, String> {

    @Query(value = "{ name: ?0 }", transform = "query-transform")
    Person findByNameTransforming(String name);

    @Query(transform = "query-transform")
    Person findFirstByOccupation(String occupation);

    @Query(transform = "query-transform")
    Page<Person> findAllBy(Pageable page);

}
