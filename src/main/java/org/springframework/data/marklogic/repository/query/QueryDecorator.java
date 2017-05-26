package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;

/**
 * For adding custom parameters to a database transform. This passes in the calling method so that the configurer can
 * use whatever information is available to create an appropriate transform for the method call.
 */
public interface QueryDecorator {

    StructuredQueryDefinition decorate(StructuredQueryDefinition query, MarkLogicQueryMethod method);
}
