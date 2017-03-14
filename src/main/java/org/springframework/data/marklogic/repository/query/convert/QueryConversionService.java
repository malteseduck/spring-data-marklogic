package org.springframework.data.marklogic.repository.query.convert;

import com.marklogic.client.query.StructuredQueryBuilder.TextIndex;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.core.convert.TypeDescriptor;

import java.util.List;
import java.util.Map;

public interface QueryConversionService {
    @SuppressWarnings("unchecked")
    StructuredQueryDefinition convert(TextIndex index, Object source, List<String> options);

    StructuredQueryDefinition convert(TextIndex index, Object source, List<String> options, TypeDescriptor sourceType);

    Map<Object, QueryTypeConverter> getConvertersToRegister();
}
