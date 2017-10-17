package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.annotation.Persistent;
import org.springframework.data.marklogic.core.convert.ServerTransformer;

import java.lang.annotation.*;

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Document
{
	/**
	 * Alias for {@link #uri()}.
	 * <p>
	 * <p>Intended to be used when no other attributes are needed, for example: {@code @Document("/articles")}.
	 *
	 * @see #uri()
	 */
	String value() default "";

	/**
	 * The base URI for documents of the annotated type.
	 */
	String uri() default "";

	/**
	 * The serialization format to use for documents of the annotated type.
	 */
	DocumentFormat format() default DocumentFormat.JSON;

	/**
	 * The name to use for the type of document which will be persisted into the database.  This overrides the default
	 * of using the class simple name (or full class name, depending on configuration).
	 *
	 * @return
	 */
	String type() default "";

	/**
	 *
	 * @return
	 */
	TypePersistenceStrategy typeStrategy() default TypePersistenceStrategy.COLLECTION;

	/**
	 * The configured transformer class to use for the entity for server read/write transforms.  An implementation of the
	 * ServerTransform interface.
	 *
	 * @return
	 */
	Class<? extends ServerTransformer> transformer() default ServerTransformer.class;
}