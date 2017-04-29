package org.springframework.data.marklogic.repository;

import org.springframework.data.marklogic.core.PersonToStream;

import java.io.InputStream;

public interface PersonStreamRepository extends MarkLogicRepository<PersonToStream, String> {

    @Query("{ name: ?0 }")
    InputStream findAllByName(String name);

    InputStream findAllByOrderByName();

    InputStream findAllByOrderByPetsNameAscNameAsc();

    InputStream findAllByGenderOrderByName(String gender);

    @Query("{ name: ?0 }")
    InputStream findAllByNameUsingGeneric(String name);
}