package org.springframework.data.marklogic.repository;

import com.marklogic.client.io.Format;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.SimplePersonTransformer;

public interface PersonTransformingRepository extends MarkLogicRepository<Person, String> {

    // Since the Person in the database is XML we need to make sure the query is built as an element query instead of JSON
    @Query(format = Format.XML)
    boolean existsByName(String name);

    @Query(value = "{ name: ?0 }", transform = "query-transform")
    Person findByNameTransforming(String name);

    @Query(transform = "query-transform")
    Person findFirstByOccupation(String occupation);

    @Query(transform = "query-transform")
    Page<Person> findAllBy(Pageable page);

    @Query(value = "{ name: ?0 }", transformer = SimplePersonTransformer.class)
    Person findByNameFullTransforming(String name);

    @Query(transformer = SimplePersonTransformer.class)
    Page<Person> queryAllBy(Pageable page);

}
