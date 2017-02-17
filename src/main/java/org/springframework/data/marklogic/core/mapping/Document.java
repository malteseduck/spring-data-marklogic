package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Document {

    TypePersistenceStrategy typeStrategy() default TypePersistenceStrategy.COLLECTION;

    DocumentFormat format() default DocumentFormat.JSON;

    // TODO: What type of object wrapping to use, FULL_CLASS, SIMPLE_CLASS, NONE, default to NONE, only configurable for JSON?
    // TODO: Whether or not to store something like "_class" so we can convert objects if type is not specified?
}