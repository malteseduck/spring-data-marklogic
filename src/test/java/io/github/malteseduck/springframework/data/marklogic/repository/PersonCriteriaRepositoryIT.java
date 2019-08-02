package io.github.malteseduck.springframework.data.marklogic.repository;


import io.github.malteseduck.springframework.data.marklogic.DatabaseConfiguration;
import io.github.malteseduck.springframework.data.marklogic.core.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class),
        @ContextConfiguration(classes = PersonCriteriaRepositoryIntegrationConfiguration.class)
})
public class PersonCriteriaRepositoryIT {

    private static final Logger log = LoggerFactory.getLogger(PersonCriteriaRepositoryIT.class);

    @Autowired
    private PersonCriteriaRepository repository;

    @Autowired
    private MarkLogicOperations operations;

    private Person bobby, george, jane, jenny, andrea, henry, freddy;
    private PersonXml jimmy;

    private List<Person> all;

    @Before
    public void init() {
        cleanDb();

        Pet fluffy = new Pet("Fluffy", "cat");
        fluffy.setImmunizations(singletonList(new Immunization("flu", "shot")));
        andrea = new Person("Andrea", 17, "female", "food prep", "There isn't much to say", Instant.parse("2016-04-01T00:00:00Z"), asList("sewing", "karate"), asList(fluffy));
        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"), asList("running", "origami"), singletonList(new Pet("Bowwow", "dog")));
        george = new Person("George", 12, "male", "engineer", "The guy who works at the gas station, he is your friend", Instant.parse("2016-02-01T00:00:00Z"), asList("fishing", "hunting", "sewing"), asList(new Pet("Hazelnut", "snake"), new Pet("Snoopy", "dog")));
        henry = new Person("Henry", 32, "male", "construction", "He built my house", Instant.parse("2016-05-01T00:00:00Z"), asList("carpentry", "gardening"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-03-01T00:00:00Z"), asList("fencing", "archery", "running"));
        jenny = new Person("Jenny", 41, "female", "dentist", "", Instant.parse("2016-06-01T00:00:00Z"), singletonList("gymnastics"), singletonList(new Pet("Powderkeg", "wolverine")));
        jenny.setModified(Instant.now().minus(1, DAYS));

        henry.setRankings(asList(1, 2, 3));

        all = (List<Person>) repository.saveAll(asList(jenny, bobby, george, jane, andrea, henry));

        freddy = new Person("Freddy", 27, "male", "policeman", "", Instant.parse("2016-08-01T00:00:00Z"), singletonList("gaming"));
        operations.write(freddy, "OtherPeople");

        jimmy = new PersonXml("Jimmy", 15, "male", "student", "Lives next door", Instant.parse("2016-12-01T00:00:00Z"));
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        repository.deleteAll();
        operations.dropCollections("OtherPeople");
    }


    @Test
    public void testFindsPersonsByName() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setName("Jan");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void testFindsPersonsByNameButIgnoresSomething() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setName("Jan");
        criteria.setSomething("Hello World");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void testFindsPersonsByExactName() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setHasExactNameOf("Jane");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void testFindsPersonsByGender() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setGender("female");

        List<Person> people = repository.findAll(criteria, Sort.by("name"));
        assertThat(people).containsExactly(andrea, jane, jenny);
    }

    @Test
    public void testFindPersonsByOccupation() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setOccupation("dentist");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactlyInAnyOrder(bobby, jenny);
    }

    @Test
    public void findsPersonsByNameNull() {
        List<Person> people = repository.findAll(new PersonCriteria());
        assertThat(people).hasSize(all.size());
    }

    @Test
    public void testFindsPersonsByAge() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setAge(23);

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testFindsPersonByBirthtime() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setBirthtime(Instant.parse("2016-01-01T00:00:00Z"));

        List<Person> people = repository.findAll(criteria);
        assertThat(people).contains(bobby);
    }

    @Test
    public void testFindsPersonBornAfter() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setBirthtime(Instant.parse("2016-01-01T00:00:00Z"));

        List<Person> people = repository.findAll(criteria);
        assertThat(people).contains(bobby);
    }
    @Test
    public void testFindsPersonsByNameLike() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setName("Bob*");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).contains(bobby);
    }

    @Test
    public void tesetExecutesAndQueryCorrectly() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setName("Bobby");
        criteria.setAge(23);

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testFindByHobbiesContains() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setHobbies("running");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactlyInAnyOrder(bobby, jane);
    }

    @Test
    public void testFindByPetName() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setPets("Powderkeg");

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByAgeGreaterThan() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setOlderThan(50);

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void testFindByBirthtimeGreaterThan() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setBornAfter(Instant.parse("2016-05-02T00:00:00Z"));

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByModifiedBefore() {
        PersonCriteria criteria = new PersonCriteria();
        criteria.setModifiedBefore(Instant.now().minus(1, HOURS));

        List<Person> people = repository.findAll(criteria);
        assertThat(people).containsExactly(jenny);
    }
}
