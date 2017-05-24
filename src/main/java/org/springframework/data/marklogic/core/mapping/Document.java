package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.annotation.Persistent;

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
	 * The database transform to use when writing this document to the database.
	 *
	 * @return
	 */
	String dbSerializer() default "";

	/**
	 * The database transform to use when reading this document from the database.
	 *
	 * @return
	 */
	String dbDeserializer() default "";

	/**
	 * The format of the document as it should be in the database.  This is for the use case of having JSON throughout the
	 * Java layer, but perhaps transforming it into XML on the server before persisting.
	 *
	 * @return
	 */
	DocumentFormat dbFormat() default DocumentFormat.JSON;

	// TODO: What type of object wrapping to use, FULL_CLASS, SIMPLE_CLASS, NONE, default to NONE, only configurable for JSON?
	// TODO: Whether or not to store something like "_class" so we can convert objects if type is not specified?
}