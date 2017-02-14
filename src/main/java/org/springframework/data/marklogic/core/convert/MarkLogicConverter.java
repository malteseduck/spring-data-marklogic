package org.springframework.data.marklogic.core.convert;

import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;

public interface MarkLogicConverter extends
        EntityConverter<MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty, Object, DocumentDescriptor>, EntityWriter<Object, DocumentDescriptor>,
        EntityReader<Object, DocumentDescriptor> {

    <T> String getDocumentUri(Object id, Class<T> entityClass);
}
