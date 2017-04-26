package org.springframework.data.marklogic.repository.query.convert;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.marklogic.repository.query.QueryType;

import java.util.List;

public interface QueryTypeConverter<S> {
    StructuredQueryDefinition convert(PropertyIndex index, S source, List<String> options);

    default boolean supports(PropertyIndex index) {
        return index.getType() == QueryType.VALUE;
    }

    // Allow for nested processing of structures - by default don't do anything and convert a single layer
    default StructuredQueryDefinition convert(PropertyIndex index, S source, List<String> options, QueryConversionService service) {
        return convert(index, source, options);
    }

}
