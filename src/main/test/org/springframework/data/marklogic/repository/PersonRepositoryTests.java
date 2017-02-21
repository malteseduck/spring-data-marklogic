package org.springframework.data.marklogic.repository;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PersonRepositoryIntegrationConfiguration.class)
public class PersonRepositoryTests {

    @Autowired
    private PersonRepository repository;

    @Autowired
    MarkLogicOperations operations;

    private Person bobby, george, jane, jenny, andrea, henry;

    List<Person> all;

    @Before
    public void setUp() {
        cleanDb();

        andrea = new Person("Andrea", 17, "female", "food prep", "There isn't much to say", Instant.parse("2016-04-01T00:00:00Z"), asList("sewing", "karate"));
        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"), asList("running", "origami"));
        george = new Person("George", 12, "male", "engineer", "The guy wo works at the gas station, he is your friend", Instant.parse("2016-02-01T00:00:00Z"), asList("fishing", "hunting", "sewing"));
        henry = new Person("Henry", 32, "male", "construction", "He built my house", Instant.parse("2016-05-01T00:00:00Z"), asList("carpentry", "gardening"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-03-01T00:00:00Z"), asList("fencing", "archery", "running"));
        jenny = new Person("Jenny", 41, "female", "nurse", "", Instant.parse("2016-06-01T00:00:00Z"), asList("gymnastics"));

        all = repository.save(asList(bobby, george, jane, andrea, henry, jenny));
    }

    @After
    public void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        repository.deleteAll();
    }


    @Test
    public void findsPersonById() throws Exception {
        Person found = repository.findOne(bobby.getId());
        assertThat(found).isEqualTo(bobby);
    }

    @Test
    public void findsAllPeople() throws Exception {
        List<Person> people = repository.findAll();
        assertThat(people).containsAll(all);
    }

    @Test
    public void findsAllWithGivenIds() {
        Iterable<Person> people = repository.findAll(asList(george.getId(), bobby.getId()));
        assertThat(people).containsExactlyInAnyOrder(george, bobby);
    }

    @Test
    public void deletesPersonCorrectly() throws Exception {
        repository.delete(george);

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(george);
    }

    @Test
    public void deletesPersonByIdCorrectly() {
        repository.delete(bobby.getId());

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void findsPersonsByName() throws Exception {
        List<Person> people = repository.findByName("Jane");
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void findsPersonsByNameIn() throws Exception {
        List<Person> people = repository.findByNameIn("Jane", "George");
        assertThat(people).containsExactlyInAnyOrder(jane, george);
    }

    @Test
    public void findsPersonsByNameNull() throws Exception {
        List<Person> people = repository.findByName(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void findsPersonsByNameInNull() throws Exception {
        List<Person> people = repository.findByNameIn(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void findsPersonsByAge() throws Exception {
        List<Person> people = repository.findByAge(23);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void findsPersonsByNameLike() throws Exception {
        List<Person> people = repository.findByNameLike("Bo*");
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void findsPagedPersons() throws Exception {
        Page<Person> page = repository.findAll(new PageRequest(1, 2, Sort.Direction.ASC, "name"));
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page).containsExactly(george, henry);
    }

    @Test
    public void executesPagedFinderCorrectly() throws Exception {
        Page<Person> page = repository.findByNameLike("*dr*",
                new PageRequest(0, 2, Sort.Direction.ASC, "name"));
        
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
        assertThat(page.getNumberOfElements()).isEqualTo(2);
        assertThat(page).containsExactly(andrea, bobby);
    }

//    @Test
//    public void executesPagedFinderWithAnnotatedQueryCorrectly() throws Exception {
//
//        Page<Person> page = repository.findByNameLikeWithPageable(".*a.*",
//                new PageRequest(0, 2, Sort.Direction.ASC, "lastname", "firstname"));
//        assertThat(page.isFirst(), is(true));
//        assertThat(page.isLast(), is(false));
//        assertThat(page.getNumberOfElements(), is(2));
//        assertThat(page, hasItems(carter, stefan));
//    }
//
//    @Test
//    public void findsPersonInAgeRangeCorrectly() throws Exception {
//
//        List<Person> people = repository.findByAgeBetween(40, 45);
//        assertThat(result.size(), is(2));
//        assertThat(result, hasItems(dave, leroi));
//    }
//

    @Test
    public void existsWorksCorrectly() {
        assertThat(repository.exists(bobby.getId())).isTrue();
    }

    @Test
    public void findsPeopleUsingNotPredicate() {
        List<Person> people = repository.findByNameNot("Andrea");
        
        assertThat(people)
                .doesNotContain(andrea)
                .hasSize(all.size() - 1);
    }

    @Test
    public void executesAndQueryCorrectly() {
        List<Person> people = repository.findByNameAndAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void executesOrQueryCorrectly() {
        List<Person> people = repository.findByNameOrAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void executesDerivedCountProjection() {
        assertThat(repository.countByName("George")).isEqualTo(1);
    }

    @Test
    public void executesDerivedExistsProjectionToBoolean() {
        assertThat(repository.existsByName("Jane")).isTrue();
        assertThat(repository.existsByName("Brunhilda")).isFalse();
    }

    @Test
    public void executesDerivedStartsWithQueryCorrectly() {
        List<Person> people = repository.findByNameStartsWith("J");
        
        assertThat(people).containsExactlyInAnyOrder(jenny, jane);
    }

    @Test
    public void executesDerivedEndsWithQueryCorrectly() {
        List<Person> people = repository.findByNameEndsWith("nny");
        assertThat(people).containsExactly(jenny);
    }
    
    @Test
    public void findByNameIgnoreCase() {
        List<Person> people = repository.findByNameIgnoreCase("george");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void findByNameNotIgnoreCase() {
        List<Person> people = repository.findByNameNotIgnoreCase("george");
        assertThat(people)
                .hasSize(all.size()-1)
                .doesNotContain(george);
    }

    @Test
    public void findByNameStartingWithIgnoreCase() {
        List<Person> people = repository.findByNameStartingWithIgnoreCase("ge");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void findByHobbiesContains() throws Exception {
        List<Person> people = repository.findByHobbiesContains(asList("running"));
        assertThat(people).containsExactlyInAnyOrder(bobby, jane);
    }

    @Test
    public void findByHobbiesNotContains() throws Exception {
        List<Person> people = repository.findByHobbiesNotContains(asList("running"));
        assertThat(people).doesNotContain(bobby, jane);
    }

    @Test
    public void findsPersonsByNameNotLike() throws Exception {
        List<Person> people = repository.findByNameNotLike("Bo*");
        assertThat(people).doesNotContain(bobby);
    }
}
