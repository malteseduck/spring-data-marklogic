package io.github.malteseduck.springframework.data.marklogic.core.mapping;

import io.github.malteseduck.springframework.data.marklogic.core.convert.ServerTransformer;
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
	 * The base URI for documents of the annotated type.  If the type persistence strategy is set to "URI" then will
     * scope all queries to limit to only documents under this URI.  Defaults to "/TYPE_NAME/".
	 */
	String uri() default "";

	/**
	 * The serialization format to use for documents of the annotated type.
	 */
	DocumentFormat format() default DocumentFormat.JSON;

	/**
	 * The name to use for the type of document which will be persisted into the database.  This overrides the default
	 * of using the class simple name (or full class name, depending on configuration).
	 */
	String type() default "";

	/**
	 * Set to scope queries to a "type" as defined by the configured strategy.
	 */
	TypePersistenceStrategy typeStrategy() default TypePersistenceStrategy.COLLECTION;

	/**
	 * The configured transformer class to use for the entity for server read/write transforms.  An implementation of the
	 * ServerTransform interface.
	 */
	Class<? extends ServerTransformer> transformer() default ServerTransformer.class;
}
