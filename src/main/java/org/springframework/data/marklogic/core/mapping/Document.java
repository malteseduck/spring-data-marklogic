package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Document {
    TypePersistenceStrategy strategy() default TypePersistenceStrategy.COLLECTION;
    DocumentFormat format() default DocumentFormat.JSON;
}