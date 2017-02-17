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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InfrastructureConfiguration.class)
public class BasicQueryTests {

    private MarkLogicTemplate template;
    private PojoQueryBuilder qb;

    private Person bobby;
    private Person george;
    private Person jane;

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

        template.write(asList(bobby, george, jane));
    }

    @After
    public void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        template.deleteAll(Person.class);
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
