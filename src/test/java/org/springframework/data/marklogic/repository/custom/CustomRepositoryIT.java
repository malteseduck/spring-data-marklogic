package org.springframework.data.marklogic.repository.custom;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.repository.config.EnableMarkLogicRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


// TODO: This test relies on the database being configured with DatabaseConfiguration so add in what is necessary to get that right here
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CustomRepositoryIT {

    @Configuration
    @EnableMarkLogicRepositories
    @ImportResource("classpath:integration.xml")
    static class Config {}

    @Autowired
    private MarkLogicOperations operations;

    @Autowired
    private PersonRepository repository;

    private Person bobby, george, jane;

    @Before
    public void init() {
        cleanDb();

        bobby = new Employee("Bobby", "Senior Engineer");
        george = new Contact("George", "801-555-5555");
        jane = new Employee("Jane", "Project Manager");

        // Use the template write() so that each entity is saved in either the "Employee" or "Contact" collections instead
        // of the "Person" collection it would if it were the repository.save().  This helps simulate querying across collections.
        operations.write(asList(jane, bobby), "Employee");
        operations.write(george, "Contact");
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        operations.deleteAll("Employee", "Contact");
    }

    @Test
    public void shouldExecuteMethodOnCustomRepositoryImplementation() {
        List<Person> people = repository.findAllPersons();

        assertThat(people).containsExactly(bobby, george, jane);
        assertThat(people.get(0)).isInstanceOf(Employee.class);
        assertThat(people.get(1)).isInstanceOf(Contact.class);
        assertThat(people.get(2)).isInstanceOf(Employee.class);
    }
}
