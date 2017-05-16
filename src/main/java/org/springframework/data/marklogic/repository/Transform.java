package org.springframework.data.marklogic.repository;

import java.lang.annotation.*;

/**
 * Allows specifying the database transform to use when returning or persisting documents through a Repository interface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface Transform {

    /**
     * The name of a transform to use when returning/saving documents (depends on the type of operations that is annotated).
     * This transform must have been previously configured through the REST API otherwise the operation will fail.
     *
     * @return
     */
    String value() default "";
}
