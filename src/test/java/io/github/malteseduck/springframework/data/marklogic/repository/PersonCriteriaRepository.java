/*
 * Copyright 2010-2017 the original author or authors.
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
package io.github.malteseduck.springframework.data.marklogic.repository;

import io.github.malteseduck.springframework.data.marklogic.core.Person;
import io.github.malteseduck.springframework.data.marklogic.core.PersonView;
import io.github.malteseduck.springframework.data.marklogic.core.Pet;
import io.github.malteseduck.springframework.data.marklogic.core.query.QueryCriteria;
import io.github.malteseduck.springframework.data.marklogic.domain.facets.FacetedPage;
import io.github.malteseduck.springframework.data.marklogic.repository.query.QueryType;
import io.github.malteseduck.springframework.data.marklogic.repository.support.QueryCriteriaExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface PersonCriteriaRepository extends MarkLogicRepository<Person, String>, QueryCriteriaExecutor<Person> {
}
