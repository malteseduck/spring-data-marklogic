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
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.repository.PersonXmlRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.QueryTestUtils.client;
import static org.springframework.data.marklogic.repository.query.QueryTestUtils.creator;
import static org.springframework.data.marklogic.repository.query.QueryTestUtils.queryMethod;

public class MarkLogicQueryCreatorXmlTests {

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
                queryMethod(PersonXmlRepository.class, "findByName", String.class),
                "Bubba"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.element("name"), "Bubba").serialize()
                );
    }

    @Test
    public void testFindByWithOrdering() throws Exception {

        StructuredQueryDefinition query = creator(
                queryMethod(PersonXmlRepository.class, "findByGenderOrderByAge", String.class),
                "female"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        operations.sortQuery(
                                new Sort("age"),
                                qb.value(qb.element("gender"), "female"),
                                Person.class
                        ).serialize()
                );
    }
}
