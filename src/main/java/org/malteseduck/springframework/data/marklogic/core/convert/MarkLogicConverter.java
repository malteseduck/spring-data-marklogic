package org.malteseduck.springframework.data.marklogic.core.convert;

import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.malteseduck.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;

import java.util.List;

public interface MarkLogicConverter extends
        EntityConverter<MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty, Object, DocumentDescriptor>, EntityWriter<Object, DocumentDescriptor>,
        EntityReader<Object, DocumentDescriptor> {

    List<String> getDocumentUris(List<?> ids);

    <T> List<String> getDocumentUris(List<?> ids, Class<T> entityClass);

    <T> QueryDefinition wrapQuery(StructuredQueryDefinition query, Class<T> entityClass);

    <R extends Object> R doRead(Class<R> type, DocumentDescriptor source);

    void doWrite(Object source, DocumentDescriptor sink);
}
