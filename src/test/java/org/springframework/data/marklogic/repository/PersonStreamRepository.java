package org.springframework.data.marklogic.repository;

import org.springframework.data.marklogic.core.PersonToStream;
import org.springframework.data.marklogic.core.mapping.DocumentStream;

import java.io.InputStream;

public interface PersonStreamRepository extends MarkLogicRepository<PersonToStream, String> {

    @Query("{ name: ?0 }")
    DocumentStream<PersonToStream> findAllByName(String name);

    DocumentStream<PersonToStream> findAllByOrderByName();

    DocumentStream<PersonToStream> findAllByOrderByPetsNameAscNameAsc();

    DocumentStream<PersonToStream> findAllByGenderOrderByName(String gender);

    @Query("{ name: ?0 }")
    InputStream findAllByNameUsingGeneric(String name);
}