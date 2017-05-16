package org.springframework.data.marklogic.repository;

import org.springframework.data.marklogic.core.Person;

public interface PersonTransformingRepository extends MarkLogicRepository<Person, String> {

    @Transform("query-transform")
    @Query("{ name: ?0 }")
    Person findByNameTransforming(String name);

    @Transform("query-transform")
    Person findFirstByOccupation(String occupation);

}
