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
import org.springframework.data.marklogic.core.Pet;
import org.springframework.data.marklogic.repository.query.QueryType;

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

    @Query(extract = {"/name", "/age"})
    List<Person> findByOccupation(String occupation);

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

    List<Person> findByPetsImmunizationsType(String type);

    List<Person> findByPetsNameIgnoreCase(String name);

    // ====== Range queries ======

    List<Person> findByAgeBetween(int from, int to);

    List<Person> findByBirthtimeGreaterThan(Instant date);

    List<Person> findByAgeLessThanEqual(int age);

    List<Person> findByAgeGreaterThanEqual(int age);

    @Query(type = QueryType.RANGE)
    List<Person> findByGender(String gender);

    // ====== Exists/count checks ======

    long countByName(String name);

    boolean existsByName(String name);

    // ====== Delete checks ======

    void deleteById(String id);

    void deleteByName(String name);

    // ====== Annotated queries (QBE) ======

    @Query("{}")
    List<Person> qbeFindAll();

    @Query("{}")
    Page<Person> qbeFindAllWithPageable(Pageable pageable);

    @Query("{ name: ?0 }")
    Person qbeFindByName(String name);

    @Query(value = "{ name: ?0 }", extract = {"/name", "/age"})
    Person qbeFindByNameExtractingNameAndAge(String name);

    @Query("{ name: ?0 }")
    List<Person> qbeFindByNameList(String name);

    @Query("{ gender: ?0 }")
    Page<Person> qbeFindByGenderWithPageable(String gender, Pageable pageable);

    @Query("{ 'name': '?0' }")
    Person qbeFindByNameQuoted(String name);

    @Query("{ 'name': 'Bobby' }")
    Person qbeFindBobby();

    @Query("{ 'pets': ?0 }")
    List<Person> qbeFindByPet(Pet pet);

    @Query("{ name: ?0, pet: ?1 }")
    Person qbeFindByLastnameAndPet(String lastname, Pet pet);

    @Query("{name: ?#{[0]} }")
    List<Person> qbeFindByQueryWithExpression(String param0);

    @Query("{id:?#{ [0] ? { $exists: {} } : [1] }}")
    List<Person> qbeFindByQueryWithExpressionAndNestedObject(boolean param0, String param1);

    @Query("{ " +
            "   gender: 'male', " +
            "   description: { $exists: {} }, " +
            "   $and: [ " +
            "       { birthtime: { $ge: '2016-02-01T00:00:00Z' } }, " +
            "       { birthtime: { $le: '2016-02-28T00:00:00Z' } }" +
            "   ], " +
            "   $word: ?0," +
            "   $filtered: false" +
            "}")
    List<Person> qbeFindByComplicated(String term);

    @Query(value = "{ $or: [{ age: ?0 }, {'age': '?0'}] }")
    boolean qbeFindByAgeQuotedAndUnquoted(int age);

    @Query("{ arg0: ?0, arg1: ?1 }")
    List<Person> qbeFindByStringWithWildcardChar(String arg0, String arg1);

    // ====== Limiting queries ======

    Person findFirstByName(String name);

    List<Person> findTop2ByOrderByName();

    Page<Person> findFirst2ByOrderByName(Pageable page);

    // ====== Streaming queries ======

    Stream<Person> readAllByAgeNotNull();

}
