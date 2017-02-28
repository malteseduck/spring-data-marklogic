package org.springframework.data.marklogic.repository;

import org.springframework.data.marklogic.core.PersonXml;

import java.util.List;

public interface PersonXmlRepository extends MarkLogicRepository<PersonXml, String> {

    List<PersonXml> findByName(String name);

    List<PersonXml> findByGenderOrderByAge(String gender);

    // Range queries
    List<PersonXml> findByAgeBetween(int from, int to);
}
