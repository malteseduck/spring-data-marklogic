package org.malteseduck.springframework.data.marklogic.core.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Indexed {
    IndexType type() default IndexType.PATH;

    /**
     * Path within the JSON/XML document to get to the property.  For example, in the following structure:
     *
     * {
     *     "name": "Bob",
     *     "pets": [
     *         { "name": "Fluffy" }
     *     ]
     * }
     *
     * the "path" would be set to "/pets" for the property "name" inside the pets array.  This is used as a hint to the
     * database sorting if you are using full path range indexes to properties.
     *
     * @return
     */
   String path() default "";

    // TODO: Info to drive automatically creating indexes?
}
