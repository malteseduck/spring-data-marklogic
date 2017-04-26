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
import org.springframework.data.marklogic.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

public class MarkLogicQueryMethod extends QueryMethod {

    private final Method method;
    private final Format format;
    private Query queryAnnotation;
    private final MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext;

    public MarkLogicQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
                            MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext) {

        super(method, metadata, projectionFactory);
        Assert.notNull(mappingContext, "MappingContext must not be null!");

        this.method = method;
        this.mappingContext = mappingContext;
        this.format = mappingContext.getPersistentEntity(getEntityInformation().getJavaType()).getDocumentFormat();
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

    QueryType getQueryType() {
        return getQueryAnnotation() != null
                ? getQueryAnnotation().type()
                : null;
    }

    /**
     * Returns the extracted properties to be used for the query.
     *
     * @return
     */
    String getExtractSpecification() {
        String value = (String) AnnotationUtils.getValue(getQueryAnnotation(), "extract");
        return StringUtils.hasText(value) ? value : null;
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
