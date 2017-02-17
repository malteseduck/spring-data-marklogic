package org.springframework.data.marklogic.repository;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Query {

    /**
     * Takes a MarkLogic QBE JSON string to define the actual query to be executed. This one will take precedence over the
     * method name then.
     *
     * @return
     */
    String value() default "";

    /**
     * Defines the properties that should be returned for the given query. Note that only these properties will make it into the
     * domain object returned.
     *
     * @return
     */
    String extract() default "";
}
