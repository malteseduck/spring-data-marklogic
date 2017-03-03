/*
 * Copyright 2011-2017 the original author or authors.
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
package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.Pet;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.repository.PersonRepository;

import java.util.List;

import static com.marklogic.client.query.StructuredQueryBuilder.Operator;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.QueryTestUtils.*;

public class MarkLogicQueryCreatorTests {

    private MarkLogicOperations operations;
    private final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Rule
    public ExpectedException expectation = ExpectedException.none();

    @Before
    public void setUp() throws SecurityException, NoSuchMethodException {
        operations = new MarkLogicTemplate(client(), new MappingMarkLogicConverter(new MarkLogicMappingContext()));
    }

    @Test
    public void testSimpleQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByName", String.class),
                "Bubba"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(qb.value(qb.jsonProperty("name"), "Bubba").serialize());
    }

    @Test
    public void testAndQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameAndAge", String.class, int.class),
                "Fred", 23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(qb.and(
                        qb.value(qb.jsonProperty("name"), "Fred"),
                        qb.value(qb.jsonProperty("age"), 23)
                ).serialize());
    }

    @Test
    public void testOrQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameOrAge", String.class, int.class),
                "John", 30
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(qb.or(
                        qb.value(qb.jsonProperty("name"), "John"),
                        qb.value(qb.jsonProperty("age"), 30)
                ).serialize());
    }

    @Test
    public void testNullQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameIsNull")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("name"), (String) null).serialize()
                );
    }

    @Test
    public void testNotNullQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameNotNull")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.not(
                                qb.value(qb.jsonProperty("name"), (String) null)
                        ).serialize()
                );
    }

    @Test
    public void testLessThanOrEqualToQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByAgeLessThanEqual", int.class),
                23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.range(qb.jsonProperty("age"), "xs:integer", (String[]) null, Operator.LE, 23).serialize()
                );
    }

    @Test
    public void testGreaterThanOrEqualToQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByAgeGreaterThanEqual", int.class),
                23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.range(qb.jsonProperty("age"), "xs:integer", (String[]) null, Operator.GE, 23).serialize()
                );
    }

    @Test
    public void testPropertyExists() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByAgeExists")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.containerQuery(qb.jsonProperty("age"), qb.and()).serialize()
                );
    }

    @Test
    public void testTruthyQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByActiveTrue")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("active"), true).serialize()
                );
    }

    @Test
    public void testFalsyQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByActiveFalse")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("active"), false).serialize()
                );
    }

    @Test
    public void testStartingWithPredicateQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameStartsWith", String.class),
                "Bob"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.word(qb.jsonProperty("name"), null, new String[]{ "wildcarded"}, 1.0, "Bob*").serialize()
                );

    }

    @Test
    public void testStartingWithWildcardedPredicateQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameStartsWith", String.class),
                "Bob*"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.word(qb.jsonProperty("name"), null, new String[]{ "wildcarded"}, 1.0, "Bob*").serialize()
                );

    }

    @Test
    public void testEndingWithPredicateQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameEndsWith", String.class),
                "by"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.word(qb.jsonProperty("name"), null, new String[]{ "wildcarded"}, 1.0, "*by").serialize()
                );
    }

    @Test
    public void testContainsStringPredicateQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByDescriptionContaining", String[].class),
                "ob"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.word(qb.jsonProperty("description"), null, new String[]{ "wildcarded"}, 1.0, "*ob*").serialize()
                );
    }

    @Test
    public void testIgnoreCase() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameIgnoreCase", String.class),
                "bobby"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("name"), null, new String[]{ "case-insensitive"}, 1.0, "bobby").serialize()
                );
    }

    @Test
    public void testNotIgnoreCase() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameNotIgnoreCase", String.class),
                "bobby"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.not(
                                qb.value(qb.jsonProperty("name"), null, new String[]{ "case-insensitive"}, 1.0, "bobby")
                        ).serialize()
                );
    }

    @Test
    public void testThrowsExceptionWithFindByIgnoreCaseOnNonString() throws Exception {
        expectation.expect(IllegalArgumentException.class);
        expectation.expectMessage("must be of type String");

        creator(
                queryMethod(PersonRepository.class, "findByAgeIgnoreCase", int.class),
                23
        ).createQuery();
    }

    @Test
    public void testOnlyGenerateLikeExpressionsForStringsIfAllIgnoreCase() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameAndAgeAllIgnoreCase", String.class, int.class),
                "bobby", 23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.and(
                                qb.value(qb.jsonProperty("name"), null, new String[]{ "case-insensitive"}, 1.0, "bobby"),
                                qb.value(qb.jsonProperty("age"), 23)
                        ).serialize()
                );
    }

    @Test
    public void testUsingContainsOnCollection() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByHobbiesContains", List.class),
                asList("fishing", "hunting")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("hobbies"), "fishing", "hunting").serialize()
                );
    }

    @Test
    public void testQueryOnLeafProperty() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByPetsName", String.class),
                "Fluffy"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.containerQuery(
                                qb.jsonProperty("pets"),
                                qb.value(qb.jsonProperty("name"), "Fluffy")
                        ).serialize()
                );
    }

    @Test
    public void testIgnoreCaseOnLeafProperty() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByPetsNameIgnoreCase", String.class),
                "fluffy"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.containerQuery(
                                qb.jsonProperty("pets"),
                                qb.value(qb.jsonProperty("name"), null, new String[]{ "case-insensitive"}, 1.0, "fluffy")
                        ).serialize()
                );
    }

    @Test
    public void testNullValueOnContainsQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByHobbiesContains", List.class),
                null
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("hobbies"), (String) null).serialize()
                );
    }

    @Test
    public void testFindByObject() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByPets", Pet.class),
                new Pet("Fluffy", "lion")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("hobbies"), (String) null).serialize()
                );
    }

    private class CoolPet extends Pet {
        int age;

        public CoolPet(String name, String type, int age) {
            super(name, type);
            this.age = age;
        }
    }

    @Test
    public void testFindByObjectWithSubclass() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByPets", Pet.class),
                new CoolPet("Fluffy", "lion", 10)
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("hobbies"), (String) null).serialize()
                );
    }

    @Test
    public void testThrowsExceptionWhenArgumentNotMatchingDeclaration() throws Exception {
        expectation.expect(IllegalArgumentException.class);
        expectation.expectMessage("Expected parameter type of " + Pet.class);

        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByPets", Pet.class),
                "Fluffy"
        ).createQuery();
    }

    @Test
    public void testFindByWithOrdering() throws Exception {

        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByGenderOrderByAge", String.class),
                "female"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        operations.sortQuery(
                                new Sort("age"),
                                qb.value(qb.jsonProperty("gender"), "female"),
                                Person.class
                        ).serialize()
                );
    }
}
