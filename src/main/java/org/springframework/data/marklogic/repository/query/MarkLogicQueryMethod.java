/*
 * Copyright 2011-2016 the original author or authors.
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
package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.io.Format;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.marklogic.domain.facets.FacetedPage;
import org.springframework.data.marklogic.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Method;

public class MarkLogicQueryMethod extends QueryMethod {

    private final Method method;
    private final Format format;
    private final Class domainClass;
    private Query queryAnnotation;
    private final MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext;

    public MarkLogicQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
                            MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext) {

        super(method, metadata, projectionFactory);
        Assert.notNull(mappingContext, "MappingContext must not be null!");

        this.method = method;
        this.mappingContext = mappingContext;
        this.domainClass = getEntityInformation().getJavaType();

        // If the query format is overridden in the @Query annotation then use that, otherwise use the entity type
        if (getQueryFormat() != Format.UNKNOWN) {
            this.format = getQueryFormat();
        } else {
            this.format = mappingContext.getPersistentEntity(domainClass).getDocumentFormat();
        }

        // QBE cannot return a FacetedPage
        Assert.isTrue(!hasAnnotatedQuery() || getAnnotatedQuery() == null || !isFacetedQuery(), "@Query queries cannot return facets");
    }

    public boolean isStreamingQuery() {
        return InputStream.class.isAssignableFrom(method.getReturnType());
    }

    public boolean isFacetedQuery() {
        return FacetedPage.class.isAssignableFrom(method.getReturnType());
    }

    public Format getFormat() {
        return format;
    }

    public boolean hasAnnotatedQuery() {
        return getAnnotatedQuery() != null;
    }

    String getAnnotatedQuery() {
        String query = (String) AnnotationUtils.getValue(getQueryAnnotation());
        return StringUtils.hasText(query) ? query : null;
    }

    String[] getQueryOptions() {
        return getQueryAnnotation() != null
                ? getQueryAnnotation().options()
                : new String[0];
    }

    String getQueryOptionsName() {
        return getQueryAnnotation() != null
                ? getQueryAnnotation().optionsName()
                : null;
    }

    QueryType getQueryType() {
        return getQueryAnnotation() != null
                ? getQueryAnnotation().type()
                : QueryType.VALUE;
    }

    Format getQueryFormat() {
        return getQueryAnnotation() != null
                ? getQueryAnnotation().format()
                : Format.UNKNOWN;
    }

    /**
     * Returns the extracted properties to be used for the query.
     *
     * @return
     */
    String[] getExtracts() {
        String[] extracts = (String[]) AnnotationUtils.getValue(getQueryAnnotation(), "extract");
        return extracts != null && extracts.length > 0 ? extracts : new String[0];
    }

    SelectedMode getSelected() {
        return getQueryAnnotation() != null ? getQueryAnnotation().selected() : SelectedMode.HIERARCHICAL;
    }

    Query getQueryAnnotation() {
        if (queryAnnotation == null) {
            queryAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
        }
        return queryAnnotation;
    }

    TypeInformation<?> getReturnType() {
        return ClassTypeInformation.fromReturnTypeOf(method);
    }

    public MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> getMappingContext() {
        return mappingContext;
    }
}
