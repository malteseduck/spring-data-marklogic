package org.springframework.data.marklogic.repository.custom;

import org.springframework.data.marklogic.core.Person;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface CustomMarkLogicRepository extends Repository<Person, String> {

    List<Person> findAllByNameCustom(String name);
}
