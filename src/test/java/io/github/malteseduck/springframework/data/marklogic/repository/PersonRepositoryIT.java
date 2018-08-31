package io.github.malteseduck.springframework.data.marklogic.repository;


import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import io.github.malteseduck.springframework.data.marklogic.core.*;
import io.github.malteseduck.springframework.data.marklogic.domain.facets.FacetResultDto;
import io.github.malteseduck.springframework.data.marklogic.domain.facets.FacetedPage;
import io.github.malteseduck.springframework.data.marklogic.repository.query.QueryTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.malteseduck.springframework.data.marklogic.DatabaseConfiguration;
import io.github.malteseduck.springframework.data.marklogic.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class),
        @ContextConfiguration(classes = PersonRepositoryIntegrationConfiguration.class)
})
public class PersonRepositoryIT {

    private static final Logger log = LoggerFactory.getLogger(PersonRepositoryIT.class);

    @Autowired
    private PersonRepository repository;

    @Autowired
    private PersonXmlRepository xmlRepository;

    @Autowired
    private PersonStreamRepository streamRepository;

    @Autowired
    private PersonTransformingRepository transRepository;

    @Autowired
    private MarkLogicOperations operations;

    private Person bobby, george, jane, jenny, andrea, henry, freddy;
    private PersonXml jimmy;

    private List<Person> all;
    private List<PersonXml> allXml;

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

        henry.setRankings(asList(1, 2, 3));

        all = (List<Person>) repository.saveAll(asList(jenny, bobby, george, jane, andrea, henry));

        freddy = new Person("Freddy", 27, "male", "policeman", "", Instant.parse("2016-08-01T00:00:00Z"), singletonList("gaming"));
        operations.write(freddy, "OtherPeople");

        jimmy = new PersonXml("Jimmy", 15, "male", "student", "Lives next door", Instant.parse("2016-12-01T00:00:00Z"));

        allXml = (List<PersonXml>) xmlRepository.saveAll(singletonList(jimmy));
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        repository.deleteAll();
        xmlRepository.deleteAll();
        operations.dropCollections("OtherPeople");
    }

    @Test
    public void testFindsPersonById() throws Exception {
        Optional<Person> found = repository.findById(bobby.getId());
        assertThat(found.get()).isEqualTo(bobby);

        try {
            found = repository.findById("does-not-exist");
            assertThat(found).isNotPresent();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Test
    public void testFindsAllPeople() throws Exception {
        List<Person> people = repository.findAll();
        assertThat(people).containsAll(all);
    }

    @Test
    public void testFindsAllPeopleOrderedByName() throws Exception {
        List<Person> people = repository.findAll(Sort.by("name"));
        assertThat(people).containsExactly(andrea, bobby, george, henry, jane, jenny);
    }

    @Test
    public void testFindsAllWithGivenIds() {
        Iterable<Person> people = repository.findAllById(asList(george.getId(), bobby.getId()));
        assertThat(people).containsExactlyInAnyOrder(george, bobby);
    }

    @Test
    public void testDeletesPersonCorrectly() throws Exception {
        repository.delete(george);

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(george);
    }

    @Test
    public void testDeleteByIdCorrectly() throws Exception {
        repository.deleteById(george.getId());

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(george);
    }

    @Test
    public void testDeletesPersonByIdCorrectly() {
        repository.deleteById(bobby.getId());

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void testFindsPersonsOrderedByName() throws Exception {
        List<Person> people = repository.findAllByOrderByNameAsc();
        assertThat(people).containsExactly(andrea, bobby, george, henry, jane, jenny);
    }

    @Test
    public void testFindsPersonsByName() throws Exception {
        List<Person> people = repository.findByName("Jane");
        assertThat(people).containsExactly(jane);
    }

    @Test
    @Ignore("projects not enabled until queries limit values returned")
    public void testFindsPersonsByNameReturningOnyName() throws Exception {
        List<PersonView> people = repository.queryByName("Jane");
        assertThat(people).extracting(PersonView::getName)
                .contains("Jane");
    }

    @Test
    public void testFindsPersonsByNameOrderedByAge() throws Exception {
        List<Person> people = repository.findByGenderOrderByAge("female");
        assertThat(people).containsExactly(andrea, jenny, jane);
    }

    @Test
    public void testFindPersonsByOccupationOrderedByName() throws Exception {
        List<Person> people = repository.findByOccupationOrderByNameAsc("dentist");
        assertThat(people).containsExactly(bobby, jenny);
    }

    @Test
    public void testFindsPersonsByNameIn() throws Exception {
        List<Person> people = repository.findByNameIn("Jane", "George");
        assertThat(people).containsExactlyInAnyOrder(jane, george);
    }

    @Test
    public void findsPersonsByNameNull() throws Exception {
        List<Person> people = repository.findByName(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void testFindsPersonsByNameInNull() throws Exception {
        List<Person> people = repository.findByNameIn((String[]) null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void testFindsPersonsByAge() throws Exception {
        List<Person> people = repository.findByAge(23);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testFindsPersonByBirthtime() throws Exception {
        Person person = repository.findByBirthtime(Instant.parse("2016-01-01T00:00:00Z"));
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindsPersonsByGenderLike() throws Exception {
        List<Person> people = repository.findByGenderLike("mal*");
        assertThat(people).containsExactlyInAnyOrder(bobby, george, henry);
    }

    @Test
    public void testFindsPersonsByNameNotLike() throws Exception {
        List<Person> people = repository.findByNameNotLike("Bob*");
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void testFindsPagedPersonsOrderedByName() throws Exception {
        Page<Person> page = repository.findAll(PageRequest.of(1, 2, Sort.Direction.ASC, "name"));
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page).containsExactly(george, henry);
    }

    @Test
    public void testHandlesNullPageable() throws Exception {
        Throwable thrown = catchThrowable(() -> repository.findAll((Pageable) null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The given Pageable must not be null");
    }

    @Test
    public void testExecutesPagedFinderCorrectly() throws Exception {
        Page<Person> page = repository.findByGenderLike("fem*",
                PageRequest.of(0, 2, Sort.Direction.ASC, "name"));

        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
        assertThat(page.getNumberOfElements()).isEqualTo(2);
        assertThat(page).containsExactly(andrea, jane);

        // Wildcard index required for result total to be correct
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void testExecutesFacetedPagedFinderCorrectly() throws Exception {
        FacetedPage<Person> page = repository.findByGenderIsLike("fem*",
                PageRequest.of(0, 2, Sort.Direction.ASC, "name"));

        assertThat(page.getNumberOfElements()).isEqualTo(2);
        assertThat(page).containsExactly(andrea, jane);

        // Wildcard index required for result total to be correct
        assertThat(page.getTotalElements()).isEqualTo(3);

        assertThat(page.getFacets())
                .extracting(FacetResultDto::getName).contains("occupation", "age", "gender");
        assertThat(page.getFacets())
                .extracting(FacetResultDto::getCount).contains(3L, 3L, 1L);
    }

    @Test
    public void testExistsWorksCorrectly() {
        assertThat(repository.existsById(bobby.getId())).isTrue();
    }

    @Test
    public void testFindsPeopleUsingNotPredicate() {
        List<Person> people = repository.findByNameNot("Andrea");

        assertThat(people)
                .doesNotContain(andrea)
                .hasSize(all.size() - 1);
    }

    @Test
    public void tesetExecutesAndQueryCorrectly() {
        List<Person> people = repository.findByNameAndAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testExecutesOrQueryCorrectly() {
        List<Person> people = repository.findByNameOrAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testExecutesDerivedCountProjection() {
        assertThat(repository.countByName("George")).isEqualTo(1);
    }

    @Test
    public void testExecutesDerivedExistsProjectionToBoolean() {
        assertThat(repository.existsByName("Jane")).as("does exist").isTrue();
        assertThat(repository.existsByName("Brunhilda")).as("doesn't exist").isFalse();
    }

    @Test
    public void testExecutesDerivedStartsWithQueryCorrectly() {
        List<Person> people = repository.findByNameStartsWith("Jen");

        assertThat(people).containsExactlyInAnyOrder(jenny);
    }

    @Test
    public void testExecutesDerivedEndsWithQueryCorrectly() {
        List<Person> people = repository.findByNameEndsWith("nny");
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByNameIgnoreCase() {
        List<Person> people = repository.findByNameIgnoreCase("george");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void testFindByNameNotIgnoreCase() {
        List<Person> people = repository.findByNameNotIgnoreCase("george");
        assertThat(people)
                .hasSize(all.size()-1)
                .doesNotContain(george);
    }

    @Test
    public void testFindByNameStartingWithIgnoreCase() {
        // Needs to be 3+ characters in order to work without filtering
        List<Person> people = repository.findByNameStartingWithIgnoreCase("geo");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void testFindByHobbiesContains() throws Exception {
        List<Person> people = repository.findByHobbiesContains(singletonList("running"));
        assertThat(people).containsExactlyInAnyOrder(bobby, jane);
    }

    @Test
    public void testFindByHobbiesNotContains() throws Exception {
        List<Person> people = repository.findByHobbiesNotContaining(singletonList("running"));
        assertThat(people).doesNotContain(bobby, jane);
    }

    @Test
    public void testFindByPet() throws Exception {
        List<Person> people = repository.findByPets(new Pet("Powderkeg", "wolverine"));
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByPetName() throws Exception {
        List<Person> people = repository.findByPetsName("Powderkeg");
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByPetImmunizationsType() throws Exception {
        List<Person> people = repository.findByPetsImmunizationsType("shot");
        assertThat(people).containsExactly(andrea);
    }

    @Test
    public void testFindByOccupationUsingExtract() {
        List<Person> people = repository.findByOccupation("engineer");

        assertThat(people).hasSize(1);

        Person person = people.get(0);
        assertThat(person.getName()).isEqualTo(george.getName());
        assertThat(person.getAge()).isEqualTo(george.getAge());
        assertThat(person.getDescription()).isNullOrEmpty();
        assertThat(person.getHobbies()).isNullOrEmpty();
        assertThat(person.getBirthtime()).isNull();
    }

    // ===== Range Queries

    @Test
    public void testFindByAgeBetween() {
        List<Person> people = repository.findByAgeBetween(20, 40);
        assertThat(people).containsExactlyInAnyOrder(bobby, henry);
    }

    @Test
    public void testFindByAgeGreaterThanEqual() {
        List<Person> people = repository.findByAgeGreaterThanEqual(50);
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void testFindByBirthtimeGreaterThan() {
        List<Person> people = repository.findByBirthtimeGreaterThan(Instant.parse("2016-05-02T00:00:00Z"));
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByGenderUsingForcedRangeQuery() {
        List<Person> people = repository.findByGender("female");
        assertThat(people).containsExactlyInAnyOrder(andrea, jane, jenny);
    }

    // ===== Limiting Queries

    @Test
    public void testFindFirstByName() {
        Person person = repository.findFirstByName("Bobby");
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindTop2ByOrderByName() {
        List<Person> people = repository.findTop2ByOrderByName();
        assertThat(people).containsExactly(andrea, bobby);
    }

    @Test
    public void testFindFirst2OrderByName() {
        Page<Person> people = repository.findFirst2ByOrderByName(PageRequest.of(0, 10, Sort.Direction.ASC, "name"));
        assertThat(people).containsExactly(andrea, bobby);
    }

    // ===== Query By Example

    @Test
    public void testFindAllQBE() throws Exception {
        List<Person> people = repository.qbeFindAll();
        assertThat(people).containsAll(all);
    }

    @Test
    public void testFindAllWithPageableQBE() throws Exception {
        Page<Person> people = repository.qbeFindAllWithPageable(
                PageRequest.of(0, 1, Sort.Direction.ASC, "name")
        );
        assertThat(people).containsExactly(andrea);
    }

    @Test
    public void testFindByNameQBE() throws Exception {
        Person person = repository.qbeFindByName("Bobby");
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindByNameExtractingQBE() throws Exception {
        Person person = repository.qbeFindByNameExtractingNameAndAge("Bobby");
        assertThat(person.getName()).isEqualTo(bobby.getName());
        assertThat(person.getAge()).isEqualTo(bobby.getAge());
        assertThat(person.getDescription()).isNullOrEmpty();
        assertThat(person.getHobbies()).isNullOrEmpty();
        assertThat(person.getBirthtime()).isNull();
    }

    @Test
    public void testFindListByNameQBE() throws Exception {
        List<Person> person = repository.qbeFindByNameList("Bobby");
        assertThat(person).containsExactly(bobby);
    }

    @Test
    public void testFindBobby() throws Exception {
        Person person = repository.qbeFindBobby();
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindByAgeExists() throws Exception {
        List<Person> person = repository.findByAgeExists();
        assertThat(person).hasSize(all.size());
    }

    @Test
    public void testFindByNameQBEQuoted() throws Exception {
        Person person = repository.qbeFindByNameQuoted("Bobby");
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindByPetQBE() throws Exception {
        List<Person> people = repository.qbeFindByPet(new Pet("Snoopy", "dog"));
        assertThat(people).containsExactly(george);
    }

    @Ignore("facets not supported from QBE endpoint")
    @Test
    public void testFindByPetPagedQBE() throws Exception {
//        FacetedPage<Person> page = repository.qbeFindByPetPaged(
//                new Pet("Snoopy", "dog"),
//                PageRequest.of(0, 1, Sort.Direction.ASC, "name"));
//        assertThat(page.getContent()).containsExactly(george);
//        assertThat(page.getFacets())
//                .extracting(FacetResultDto::getName).contains("occupation", "age", "gender");
//        assertThat(page.getFacets().stream().flatMap(result -> result.getValues().stream()).collect(Collectors.toList()))
//                .extracting(FacetValueDto::getName).contains("", "23", "");
    }

    @Test
    public void testFindByGenderWithPageableQBE() throws Exception {
        Page<Person> people = repository.qbeFindByGenderWithPageable(
                "female",
                PageRequest.of(0, 2, Sort.Direction.ASC, "name")
        );
        assertThat(people).containsExactly(andrea, jane);
    }

    @Test
    public void testFindByAgeQuotedAndUnquotedQBE() throws Exception {
        List<Person> people = repository.qbeFindByAgeQuotedAndUnquoted(23);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testFindBy() throws Exception {
        List<Person> people = repository.qbeFindByComplicated("fish");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void testFindByGenderQBEHonorsCollections() throws Exception {
        Page<Person> people = repository.qbeFindByGenderWithPageable(
                "male",
                PageRequest.of(0, 20, Sort.Direction.ASC, "name")
        );
        assertThat(people).containsExactly(bobby, george, henry);
    }

    // ===== XML Query By Example

    @Test
    public void testFindXmlByNameQBE() throws Exception {
        List<PersonXml> people = xmlRepository.qbeFindByName("Jimmy");
        assertThat(people).containsExactly(jimmy);
    }

    @Test
    public void testFindXmlByNameQBEWithoutSpecifyingFormat() throws Exception {
        List<PersonXml> people = xmlRepository.qbeFindByNameWithoutSpecifyingFormat("Jimmy");
        assertThat(people).containsExactly(jimmy);
    }

    // ===== Various forms of streaming

    @Test
    public void testFindByNameReturningDocumentStream() {
        InputStream people = streamRepository.findAllByName("Bobby");
        assertThat(people).hasSameContentAs(QueryTestUtils.stream(bobby));
    }

    @Test
    public void testFindByNameReturningInputStream() {
        InputStream people = streamRepository.findAllByNameUsingGeneric("Bobby");
        assertThat(people).hasSameContentAs(QueryTestUtils.stream(bobby));
    }

    @Test
    public void testFindAllByOrderByNameReturningDocumentStream() {
        InputStream people = streamRepository.findAllByOrderByName();
        assertThat(people).hasSameContentAs(QueryTestUtils.stream(andrea, bobby, george, henry, jane, jenny));
    }

    @Test
    public void testFindAllByOrderByPetsNameReturningDocumentStream() {
        InputStream people = streamRepository.findAllByOrderByPetsNameAscNameAsc();
        assertThat(people).hasSameContentAs(QueryTestUtils.stream(bobby, andrea, george, jenny, henry, jane));
    }

    @Test
    public void testFindAllByGenderReturningDocumentStream() {
        InputStream people = streamRepository.findAllByGenderOrderByName("female");
        assertThat(people).hasSameContentAs(QueryTestUtils.stream(andrea, jane, jenny));
    }

    @Test
    @Ignore("not yet fully implemented?")
    public void testFindAllXmlReturningStream() {
        InputStream people = xmlRepository.findAllByName("Jimmy");
        assertThat(people).hasSameContentAs(QueryTestUtils.streamXml(jimmy));
    }

    // ====== Transforming Queries =====

    @Test // spring-data-marklogic/issues/8
    public void testFindByPersonNameAsElementValueQuery() {
        operations.execute((manager, transaction) -> {
            DocumentMetadataHandle meta = new DocumentMetadataHandle();
            meta.getCollections().addAll("Person");
            manager.write(
                    "/Person/testPerson.xml",
                    meta,
                    new StringHandle("" +
                            "<person>" +
                            "   <id>testPerson</id>" +
                            "   <name>Bubba</name>" +
                            "</person>").withFormat(Format.XML),
                    transaction
            );

            return null;
        });

        boolean exists = transRepository.existsByName("Bubba");

        assertThat(exists).isTrue();
    }

    @Test
    public void testFindPersonAndTransform() {
        Person person = transRepository.findByNameTransforming("Bobby");
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("Override Master Read");
    }

    @Test
    public void testFindAllOverriddenWithTransform() {
        Page<Person> results = transRepository.findAllBy(PageRequest.of(0, 1));
        assertThat(results).isNotEmpty();

        Person person = results.iterator().next();
        assertThat(person.getName()).isEqualTo("Override Master Read");
    }

    @Test
    public void testFindPersonWithFullTransform() {
        Person person = transRepository.findByNameFullTransforming("Bobby");
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("Override Master Read");
    }

    @Test
    public void testFindAllOverriddenWithFullTransform() {
        Page<Person> results = transRepository.queryAllBy(PageRequest.of(0, 1));
        assertThat(results).isNotEmpty();

        Person person = results.iterator().next();
        assertThat(person.getName()).isEqualTo("Override Master Read");
    }

    @Ignore("Maybe not possible?")
    @Test
    public void testDefaultImplementationFind() {
        Page<Person> results = transRepository.findAllBy(PageRequest.of(0, 1));

        assertThat(results).isNotEmpty();

        Person person = results.iterator().next();

        assertThat(person.getName()).isEqualTo("Override Master Read");
    }

    @Test
    public void testFindPersonWithStructuredQueryAndTransform() {
        Person person = transRepository.findFirstByOccupation("construction");
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("Override Master Read");
    }
}
