package org.springframework.data.marklogic.core.convert;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;

import java.util.List;

public interface MarkLogicConverter extends
        EntityConverter<MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty, Object, DocumentDescriptor>, EntityWriter<Object, DocumentDescriptor>,
        EntityReader<Object, DocumentDescriptor> {


    List<String> getDocumentUris(List<?> ids);

    <T> List<String> getDocumentUris(List<?> ids, Class<T> entityClass);

    <T> StructuredQueryDefinition wrapQuery(StructuredQueryDefinition query, Class<T> entityClass);

    String getTypeName(MarkLogicPersistentEntity entity);
}
