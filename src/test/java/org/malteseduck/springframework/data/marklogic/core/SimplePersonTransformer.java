package org.malteseduck.springframework.data.marklogic.core;

import com.marklogic.client.document.ServerTransform;
import org.malteseduck.springframework.data.marklogic.core.convert.ServerTransformer;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.repository.query.ParameterAccessor;

public class SimplePersonTransformer implements ServerTransformer {

    @Override
    public <T> ServerTransform reader(MarkLogicPersistentEntity<T> pEntity, ParameterAccessor params) {
        return new ServerTransform("query-transform")
                .addParameter("unused", "no, really, unused");
    }

    @Override
    public <T>ServerTransform writer(MarkLogicPersistentEntity<T> pEntity) {
        return new ServerTransform("write-transform");
    }
}
