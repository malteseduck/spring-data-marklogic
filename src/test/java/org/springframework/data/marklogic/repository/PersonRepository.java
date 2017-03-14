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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.Pet;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface PersonRepository extends MarkLogicRepository<Person, String> {

    List<Person> findAllByOrderByNameAsc();

    List<Person> findByName(String name);

    List<Person> findByGenderOrderByAge(String gender);

    List<Person> findByNameStartsWith(String prefix);

    List<Person> findByNameStartingWithIgnoreCase(String prefix);

    List<Person> findByNameEndsWith(String postfix);

    List<Person> findByOccupationOrderByNameAsc(String occupation);

    List<Person> findByGenderLike(String gender);

    List<Person> findByNameNotLike(String name);

    List<Person> findByNameLikeOrderByNameAsc(String name, Sort sort);

    List<Person> findByNameNot(String name);

    List<Person> findByNameIsNull();

    List<Person> findByNameNotNull();

    List<Person> findByActiveTrue();

    List<Person> findByActiveFalse();

    List<Person> findByNameIgnoreCase(String name);

    List<Person> findByNameNotIgnoreCase(String name);

    List<Person> findByAgeIgnoreCase(int age);

    List<Person> findByDescriptionContaining(String... words);

    List<Person> findByDescriptionNotContains(String... words);

    List<Person> findByHobbiesContains(List<String> hobbies);

    List<Person> findByRankingsContains(List<Integer> ratings);

    List<Person> findByHobbiesNotContaining(List<String> hobbies);

    Page<Person> findByGenderLike(String gender, Pageable pageable);

    List<Person> findByNameIn(String... names);

    List<Person> findByNameNotIn(Collection<String> names);

    List<Person> findByNameAndAge(String name, int age);

    List<Person> findByNameAndAgeAllIgnoreCase(String name, int age);

    List<Person> findByNameOrAge(String name, int age);

    List<Person> findByAge(int age);

    Person findByBirthtime(Instant birthtime);

    List<Person> findByAgeExists();

    List<Person> findByPets(Pet pet);

    List<Person> findByPetsName(String name);

    List<Person> findByPetsNameIgnoreCase(String name);

    // ====== Range queries ======
    List<Person> findByAgeBetween(int from, int to);

    List<Person> findByBirthtimeLessThan(Instant date);

    List<Person> findByBirthtimeGreaterThan(Instant date);

    List<Person> findByAgeLessThanEqual(int age);

    List<Person> findByAgeGreaterThanEqual(int age);

    // ====== Exists/count checks ======
    long countByName(String name);

    boolean existsByName(String name);

    // ====== Delete checks ======
    // TODO: Implement delete-by-query logic?  Require uri lexicon?
    void deleteById(String id);

    void deleteByName(String name);

    // ====== Annotated queries (QBE) ======
    @Query("{ name: ?0 }")
    Person qbeFindByName(String name);

    @Query("{ gender: ?0 }")
    Page<Person> qbeFindByGenderWithPageable(String gender, Pageable pageable);

    @Query("{ 'name': '?0' }")
    Person qbeFindByNameQuoted(String name);

    @Query("{ 'pets': ?0 }")
    List<Person> qbeFindByPet(Pet pet);

    @Query("{ name: ?0, pet: ?1 }")
    Person qbeFindByLastnameAndPet(String lastname, Pet pet);

    @Query("{name: ?#{[0]} }")
    List<Person> qbeFindByQueryWithExpression(String param0);

    @Query("{id:?#{ [0] ? { $exists: {} } : [1] }}")
    List<Person> qbeFindByQueryWithExpressionAndNestedObject(boolean param0, String param1);

    @Query(value = "{ $or: [{ age: ?0 }, {'age': '?0'}] }")
    boolean qbeFindByAgeQuotedAndUnquoted(int age);

    @Query("{ arg0: ?0, arg1: ?1 }")
    List<Person> qbeFindByStringWithWildcardChar(String arg0, String arg1);

    // ====== Limiting queries ======
    // TODO: Implement limiting query logic
    Person findFirstByName(String name);

    List<Person> findTop3ByNameOrderByName(String name);

    Page<Person> findFirst2ByNameOrderByName(String name, Pageable page);

    Slice<Person> findTop1ByNameOrderByName(String name, Pageable page);

    // ====== Streaming queries ======

    Stream<Person> readAllByAgeNotNull();
}
