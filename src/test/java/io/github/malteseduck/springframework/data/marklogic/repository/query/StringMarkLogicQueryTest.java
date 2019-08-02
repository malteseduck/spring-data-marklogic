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
package io.github.malteseduck.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.junit.Before;
import org.junit.Test;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import io.github.malteseduck.springframework.data.marklogic.core.Pet;
import io.github.malteseduck.springframework.data.marklogic.core.convert.JacksonMarkLogicConverter;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import io.github.malteseduck.springframework.data.marklogic.repository.PersonRepository;
import io.github.malteseduck.springframework.data.marklogic.repository.PersonXmlRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.malteseduck.springframework.data.marklogic.repository.query.QueryTestUtils.*;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Tests all the QBE queries.  You may notice that some of the properties in the "expected" JSON contain an '%' character.  These will be
 * stripped out during the comparison and are there so "valid" MarkLogic JSON can be created from a structure that may contain duplicate keys.
 * Duplicate keys are allowed in MarkLogic, but not in many JSON creators/parsers.  Single quotes are converted to double quotes as well.  This was
 * all done so the tests are more readable.
 */
public class StringMarkLogicQueryTest {

	SpelExpressionParser PARSER = new SpelExpressionParser();

	MarkLogicOperations operations;

	@Before
	public void setUp() {
		operations = new MarkLogicTemplate(client(), new JacksonMarkLogicConverter(new MarkLogicMappingContext()));
	}

	@Test
	public void bindsSimplePropertyCorrectly() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByName", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ $query: { name: 'Bubba' } }"));
	}

	@Test
	public void bindsSimplePropertyWithPageable() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByGenderWithPageable", String.class, Pageable.class),
				"female", PageRequest.of(0, 2, ASC, "name")
		);
		// TODO: Should this be a multipart string?
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { gender: 'female' }, options: { 'sort-order': { direction : 'ascending', 'path-index': { text: '/name' } } } } }"));
	}

	@Test
	public void bindsSimplePropertyWithPageableElementSort() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByGenderWithPageable", String.class, Pageable.class),
				"female", PageRequest.of(0, 2, ASC, "description")
		);
		// TODO: Should this be a multipart string?
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { gender: 'female' }, options: { 'sort-order': { direction: 'ascending', element: { ns: '', name: 'description' } } } } }"));
	}

	@Test
	public void bindsSimplePropertyWithPageableMultipleSort() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByGenderWithPageable", String.class, Pageable.class),
				"female", PageRequest.of(0, 2, Sort.by(ASC, "name").and(Sort.by(DESC, "age")))
		);
		// TODO: Should this be a multipart string?
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { gender: 'female'}, options: { 'sort-order': { direction: 'ascending', 'path-index': { text: '/name' } }, 'sort-order%': { direction: 'descending', path-index: { text: '/age' } } } } }"));
	}

	@Test
	public void bindsComplexPropertyCorrectly() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByPet", Pet.class),
				new Pet("Fluffy", "cat")
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ $query: { pets: { name: 'Fluffy', type: 'cat', immunizations: null } } }"));
	}

	@Test
	public void testConvertsToProperJSON() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByName", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ $query: { name: 'Bubba' } }"));
	}

	@Test
	public void testOmitsOptionsToSupportPersistedOptions() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindBobby")
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ $query: { name: 'Bobby' } }"));
	}


	@Test
	public void testConvertsToProperXML() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonXmlRepository.class, "qbeFindByName", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo("<search xmlns=\"http://marklogic.com/appservices/search\"><q:qbe xmlns:q=\"http://marklogic.com/appservices/querybyexample\"><q:query xmlns=\"\"><name>Bubba</name></q:query></q:qbe></search>");
	}

	@Test
	public void testBindsSimplePropertyAlreadyQuotedCorrectly() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByNameQuoted", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ $query: { name: 'Bubba' } }"));

	}
}
