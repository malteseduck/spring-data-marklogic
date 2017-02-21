/*
 * Copyright 2010-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.marklogic.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.Person;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PersonRepository extends MarkLogicRepository<Person, String> {

    List<Person> findByName(String name);

    List<Person> findByNameStartsWith(String prefix);

    List<Person> findByNameStartingWithIgnoreCase(String prefix);

    List<Person> findByNameEndsWith(String postfix);

    List<Person> findByNameOrderByAgeAsc(String name);

    List<Person> findByNameLike(String name);

    List<Person> findByNameNot(String name);

    List<Person> findByNameIgnoreCase(String name);

    List<Person> findByNameNotIgnoreCase(String name);

    List<Person> findByNameNotContains(String name);

    List<Person> findByNameNotLike(String name);

    List<Person> findByNameLikeOrderByNameAsc(String name, Sort sort);

    List<Person> findByHobbiesContains(List<String> hobbies);

    List<Person> findByHobbiesNotContains(List<String> hobbies);

    Page<Person> findByNameLike(String name, Pageable pageable);

    List<Person> findByNameIn(String... names);

    List<Person> findByNameNotIn(Collection<String> names);

    List<Person> findByNameAndAge(String name, int age);

    List<Person> findByNameOrAge(String name, int age);

    List<Person> findByAge(int age);

    List<Person> findByAgeBetween(int from, int to);

    List<Person> findByBirthtimeLessThan(Instant date);

    List<Person> findByBirthtimeGreaterThan(Instant date);

    // Annotated queries (QBE)
    @Query(value = "{ 'name' : ?0 }", extract = "[ '/firstname', '/description' ]")
    List<Person> findByThePersonsName(String name);

    @Query("{'age' : { '$lt' : ?0 } }")
    List<Person> findByAgeLessThan(int age, Sort sort);

    // Exists/count checks
    long countByName(String name);

    boolean existsByName(String name);

}
