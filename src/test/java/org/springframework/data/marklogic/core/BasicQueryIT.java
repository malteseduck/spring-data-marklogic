package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.pojo.PojoQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.marklogic.InfrastructureConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InfrastructureConfiguration.class)
public class BasicQueryIT {

    private MarkLogicTemplate template;
    private PojoQueryBuilder qb;

    private Person bobby, george, jane;
    private List<Person> all;

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
        qb = template.queryBuilder(Person.class);
    }

    @Before
    public void setUp() {
        cleanDb();

        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"));
        george = new Person("George", 12, "male", "engineer", "The guy wo works at the gas station, he is your friend", Instant.parse("2016-01-01T00:00:00Z"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-01-01T00:00:00Z"));

        all = template.write(asList(bobby, george, jane));
    }

    @After
    public void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        template.deleteAll(Person.class);
    }

    @Test
    public void testExists() throws Exception {
        assertThat(template.exists(qb.value("age", 23), Person.class)).as("does exist").isTrue();
        assertThat(template.exists(qb.value("occupation", "knight"), Person.class)).as("doesn't exist").isFalse();
    }

    @Test
    public void testExistsById() throws Exception {
        assertThat(template.exists(bobby.getId())).as("does exist").isTrue();
        assertThat(template.exists("invalidid")).as("doesn't exist").isFalse();
    }

    @Test
    public void testCount() throws Exception {
        assertThat(template.count(Person.class)).isEqualTo(all.size());
    }

    @Test
    public void testCountByCollections() throws Exception {
        assertThat(template.count("Person")).isEqualTo(all.size());
    }

    @Test
    public void testCountByQuery() throws Exception {
        assertThat(template.count(qb.value("gender", "male"))).as("without type").isEqualTo(2);
        assertThat(template.count(qb.value("gender", "male"), Person.class)).as("with type").isEqualTo(2);
    }

    @Test
    public void testQueryByValue() {
        List<Person> people = template.search(
            qb.value("gender", "male"),
            Person.class
        );

        assertThat(people).hasSize(2);
    }

    @Test
    public void testQueryByValueWithLimit() {
        Page<Person> people = template.search(
            qb.value("gender", "male"),
            0,
            1,
            Person.class
        );

        assertThat(people).hasSize(1);
        assertThat(people.getTotalPages()).isEqualTo(2);
    }

    @Ignore("not yet implemented - requires range index")
    @Test
    public void testQuerySorted() {
        List<Person> people = template.search(qb.and(), Person.class);

        assertThat(people).containsExactly(bobby, george, jane);
    }

    @Ignore("not yet implemented - requires range index")
    @Test
    public void testQueryByValueSorted() {
        List<Person> people = template.search(qb.value("gender", "male"), Person.class);

        assertThat(people).containsExactly(bobby, george, jane);
    }

    @Ignore("not yet implemented")
    @Test
    public void testQueryByValueReturningFacets() {
//        FacetResult facets = template.values("/occupation", qb.value("gender", "male"));
//
//        assertThat(facets.map(FacetValue::getName))
//                .containsExactlyInAnyOrder(george.getOccupation(), bobby.getOccupation())
    }


}
