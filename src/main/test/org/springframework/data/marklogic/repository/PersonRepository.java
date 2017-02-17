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

/**
 * Sample repository managing {@link Person} entities.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Fırat KÜÇÜK
 * @author Mark Paluch
 */
public interface PersonRepository extends MarkLogicRepository<Person, String> {

    List<Person> findByName(String name);

    List<Person> findByNameStartsWith(String prefix);

    List<Person> findByNameEndsWith(String postfix);

    List<Person> findByNameOrderByAgeAsc(String name);

    @Query(value = "{ 'name' : ?0 }", extract = "[ '/firstname', '/description' ]")
    List<Person> findByThePersonsName(String name);

    List<Person> findByNameLike(String name);

    List<Person> findByNameNotContains(String name);

    List<Person> findByNameNotLike(String name);

    List<Person> findByNameLikeOrderByNameAsc(String name, Sort sort);

    List<Person> findByHobbiesContains(List<String> hobbies);

    List<Person> findByHobbiesNotContains(List<String> hobbies);

    @Query("{'age' : { '$lt' : ?0 } }")
    List<Person> findByAgeLessThan(int age, Sort sort);

    Page<Person> findByNameLike(String name, Pageable pageable);

    List<Person> findByNameIn(String... names);

    List<Person> findByNameNotIn(Collection<String> names);

    List<Person> findByNameAndAge(String name, String age);

    List<Person> findByAgeBetween(int from, int to);

    List<Person> findByNamedQuery(String firstname);

    List<Person> findByBirthtimeAtLessThan(Instant date);

    List<Person> findByBirthtimeAtGreaterThan(Instant date);

    long countByName(String name);

    boolean existsByName(String name);
}
