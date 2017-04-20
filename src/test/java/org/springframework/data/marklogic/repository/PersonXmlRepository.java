package org.springframework.data.marklogic.repository;

import com.marklogic.client.io.Format;
import org.springframework.data.marklogic.core.PersonXml;

import java.util.List;

public interface PersonXmlRepository extends MarkLogicRepository<PersonXml, String> {

    List<PersonXml> findByName(String name);

    List<PersonXml> findByGenderOrderByAge(String gender);

    // Range queries
    List<PersonXml> findByAgeBetween(int from, int to);

    // QBE
    @Query(value = "{ name: ?0 }", format = Format.XML)
    List<PersonXml> qbeFindByName(String name);

    @Query(value = "{ name: ?0 }")
    List<PersonXml> qbeFindByNameWithoutSpecifyingFormat(String name);
}
