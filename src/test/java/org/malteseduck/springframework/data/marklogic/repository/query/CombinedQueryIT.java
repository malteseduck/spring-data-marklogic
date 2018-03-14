package org.malteseduck.springframework.data.marklogic.repository.query;


import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.StructuredQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import org.malteseduck.springframework.data.marklogic.core.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:integration.xml")
public class CombinedQueryIT {

    private MarkLogicTemplate template;

    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
    }

    @Before
    public void init() {
        cleanDb();
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        template.dropCollection(Person.class);
    }

    @Test
    public void testQueryAll() {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        template.write(asList(bob, george));

        List<Person> people = template.search(combine(), Person.class);
        assertThat(people).containsExactlyInAnyOrder(bob, george);
    }

    @Test
    public void testQueryJustOne() {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        template.write(asList(bob, george));

        List<Person> people = template.search(
                combine(qb.value(qb.jsonProperty("name"), "Bob")),
                Person.class
        );
        assertThat(people).containsExactly(bob);
    }

}