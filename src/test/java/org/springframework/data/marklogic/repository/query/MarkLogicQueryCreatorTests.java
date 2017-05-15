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
import org.springframework.data.marklogic.core.*;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.repository.PersonRepository;
import org.springframework.data.marklogic.repository.PersonStreamRepository;
import org.springframework.data.marklogic.repository.PersonXmlRepository;

import java.time.Instant;
import java.util.List;

import static com.marklogic.client.query.StructuredQueryBuilder.Operator;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.QueryTestUtils.*;
import static org.springframework.data.marklogic.repository.query.StubParameterAccessor.getAccessor;

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
                        qb.range(qb.pathIndex("/age"), "xs:int", (String[]) null, Operator.LE, 23).serialize()
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
                        qb.range(qb.pathIndex("/age"), "xs:int", (String[]) null, Operator.GE, 23).serialize()
                );
    }

    @Test
    public void testGreaterThanQueryWithSpecifiedIndexType() throws Exception {
        Instant from = Instant.now();
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByBirthtimeGreaterThan", Instant.class),
                from
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.range(qb.jsonProperty("birthtime"), "xs:dateTime", (String[]) null, Operator.GT, from).serialize()
                );
    }

    @Test
    public void testForceRangeQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByGender", String.class),
                "Bubba"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.range(qb.pathIndex("/gender"), "xs:string", (String[]) null, Operator.EQ, "Bubba").serialize()
                );
    }

    @Test
    public void testBetweenRangeQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByAgeBetween", int.class, int.class),
                20, 30
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.and(
                            qb.range(qb.pathIndex("/age"), "xs:int", (String[]) null, Operator.GE, 20),
                            qb.range(qb.pathIndex("/age"), "xs:int", (String[]) null, Operator.LE, 30)
                        ).serialize()
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
        expectation.expect(IllegalArgumentException.class);
        expectation.expectMessage("must be of type String");

        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameAndAgeAllIgnoreCase", String.class, int.class),
                "bobby", 23
        ).createQuery();
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
    public void testUsingContainsOnIntegerCollection() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByRankingsContains", List.class),
                asList(1, 2)
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("rankings"), 1, 2).serialize()
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
    public void testQueryOnDeepLeafProperty() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByPetsImmunizationsType", String.class),
                "shot"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.containerQuery(
                                qb.jsonProperty("pets"),
                                qb.containerQuery(
                                        qb.jsonProperty("immunizations"),
                                        qb.value(qb.jsonProperty("type"), "shot")
                                )
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
                (Object) null
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
                        qb.containerQuery(qb.jsonProperty("pets"), qb.and(
                                qb.value(qb.jsonProperty("name"), "Fluffy"),
                                qb.value(qb.jsonProperty("type"), "lion"),
                                qb.value(qb.jsonProperty("immunizations"), new String[]{null})
                        )).serialize()
                );
    }

    private class CoolPet extends Pet {
        int age;

        public CoolPet(String name, String type, int age) {
            super(name, type);
            this.age = age;
        }

        public int getAge() {
            return age;
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
                        qb.containerQuery(qb.jsonProperty("pets"), qb.and(
                                qb.value(qb.jsonProperty("name"), "Fluffy"),
                                qb.value(qb.jsonProperty("type"), "lion"),
                                qb.value(qb.jsonProperty("immunizations"), new String[]{ null }),
                                qb.value(qb.jsonProperty("age"), 10)
                        )).serialize()
                );
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

    @Test
    public void testFindByWithOrderingBySpecifiedPath() throws Exception {

        StructuredQueryDefinition query = creator(
                queryMethod(PersonStreamRepository.class, "findAllByOrderByPetsNameAscNameAsc")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        CombinedQueryDefinitionBuilder
                                .combine()
                                .options("<sort-order direction='ascending'><path-index>/pets/name</path-index></sort-order>")
                                .options("<sort-order direction='ascending'><element ns='' name='name'/></sort-order>")
                                .serialize()
                );
    }

    @Test
    public void testDeleteByField() throws Exception {

        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "deleteById", String.class),
                "23"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("id"), "23").serialize()
                );
    }

    @Test
    public void testSimpleXmlQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonXmlRepository.class, "findByName", String.class),
                "Bubba"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.element("name"), "Bubba").serialize()
                );
    }

    @Test
    public void testFindXmlByWithOrdering() throws Exception {

        StructuredQueryDefinition query = creator(
                queryMethod(PersonXmlRepository.class, "findByGenderOrderByAge", String.class),
                "female"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        operations.sortQuery(
                                new Sort("age"),
                                qb.value(qb.element("gender"), "female"),
                                PersonXml.class
                        ).serialize()
                );
    }

    @Test
    public void testQueryLimiting() throws Exception {

        StructuredQueryDefinition query = tree(
                queryMethod(PersonRepository.class, "findFirstByName", String.class)
        ).createQuery(getAccessor("Bobby"));
        assertThat(query.serialize())
                .isEqualTo(
                        new CombinedQueryDefinitionBuilder(
                                qb.value(qb.jsonProperty("name"), "Bobby")
                        ).limit(1).serialize()
                );
    }

    @Test
    public void testLimitingWithOrderBy() throws Exception {

        StructuredQueryDefinition query = tree(
                queryMethod(PersonRepository.class, "findTop2ByOrderByName")
        ).createQuery(getAccessor());
        assertThat(query.serialize())
                .isEqualTo(
                        ((CombinedQueryDefinition)
                                operations.sortQuery(
                                        new Sort("name"),
                                        null,
                                        Person.class
                                )
                        ).limit(2).serialize()
                );
    }
}
