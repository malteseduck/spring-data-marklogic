package org.springframework.data.marklogic.repository;

import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.marklogic.repository.query.QueryDecorator;

import java.lang.annotation.*;

/**
 * Allows application of a query "decorator" that can add common query logic to all the structured queries being sent to
 * the database, i.e. common security logic, transforms with parameters, collections, etc.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Decorate {

    /**
     * Allows the specification of a decorator class that will add a set piece of query logic to the StructuredQueryDefinition
     * being created by the finder query to which this is attached.
     *
     * @return
     */
    Class<? extends QueryDecorator> using();
}
