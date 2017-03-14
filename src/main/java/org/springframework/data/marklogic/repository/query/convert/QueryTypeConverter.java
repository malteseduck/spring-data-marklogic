package org.springframework.data.marklogic.repository.query.convert;

import com.marklogic.client.query.StructuredQueryBuilder.TextIndex;
import com.marklogic.client.query.StructuredQueryDefinition;

import java.util.List;

public interface QueryTypeConverter<S> {
    StructuredQueryDefinition convert(TextIndex index, S source, List<String> options);

    // Allow for nested processing of structures - by default don't do anything and convert a single layer
    default StructuredQueryDefinition convert(TextIndex index, S source, List<String> options, QueryConversionService service) {
        return convert(index, source, options);
    }
}
