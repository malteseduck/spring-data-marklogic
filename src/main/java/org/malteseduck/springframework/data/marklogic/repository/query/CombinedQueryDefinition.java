package org.malteseduck.springframework.data.marklogic.repository.query;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.Format;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.malteseduck.springframework.data.marklogic.core.mapping.IndexType;

import java.util.List;

/**
 * Convenience interface provided to allow easy creation of "combined" queries.  These type of queries combined structured
 * queries and ad-hoc options (not persisted to the database) (see http://docs.marklogic.com/guide/rest-dev/search#id_69918)
 * for more information.
 *
 * This interface follows the Java "builder" pattern to make it easier to construct queries.  As much as possible any
 * defaults for these methods follow conventions used in the MarkLogic Java Client Library.
 *
 * This interface also has helper methods to add some of the more common query options like sorting, extractions, and
 * term searches.
 */
public interface CombinedQueryDefinition extends StructuredQueryDefinition {

    /**
     * Gets the raw Query By Example definition from the combined query.
     *
     * @return the raw QBE object.
     */
    RawQueryByExampleDefinition getRawQbe();

    /**
     * Adds a raw Query By Example definition to the current combined query.
     *
     * @see CombinedQueryDefinitionBuilder#byExample(RawQueryByExampleDefinition, Format)
     */
    CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe);

    /**
     * Adds a raw Query By Example definition to the current combined query.  This will supercede any structured query
     * that currently exists in the combined query, so use one or the other.
     *
     * @param qbe the raw QBE object.
     * @param format Either JSON or XML, using the {@link com.marklogic.client.io.Format} enum.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe, Format format);

    /**
     * Combine the current structured queries in the combined query with the specified queries using an "and".
     *
     * @param queries Queries to add to the combined query
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition and(StructuredQueryDefinition... queries);

    /**
     * Combine the current structured queries in the combined query with the specified queries using an "or".
     *
     * @param queries Queries to add to the combined query
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition or(StructuredQueryDefinition... queries);

    /**
     * Adds constraints to limit results to only documents contained in one of the specified collections.  This can be
     * called multiple times and the result is additive.
     *
     * By default all documents are in a collection named after the entity class, i.e. a Person entity is stored in a
     * collection with the URI "Person".
     *
     * @param collections A list of collection URIs
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition collections(String... collections);

    /**
     * Adds contraints to limit results to only documents contained in the specified directory.  Multiple calls to this
     * function will replace the value of previous calls.
     *
     * By default all documents are stored under a path named after the entity class (to avoid ID clashes, since the
     * database URI is the "true" primary key).  So a Person entity is stored under the path "/Person/".
     *
     * @param directory The directory path under which to limit results.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition directory(String directory);

    /**
     * Sets the query to use persisted options that have the specified name.  If you find you are adding lots of ad-hoc
     * options to your queries or that you are having performance issues because of building them each time you can save
     * those options and reference them in your query.
     *
     * For more information see http://docs.marklogic.com/guide/java/query-options#id_20346.
     *
     * @param name The name of the options as they are saved in the database.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition optionsName(String name);

    /**
     * Add a snippet of XML query options to the combined query.  If you wanted to configure the query to return the
     * metrics of the query you would call the method like so:
     *
     *      combinedQuery.options("<return-metrics>true</return-metrics>");
     *
     * @param options Variable number of options XML strings to add to the query.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition options(String... options);

    /**
     * Add sorting configuration to the query.  The default sort algorithm will expect
     * to use a path range index, i.e. if sorting on "name" then a path index of "/name" should exist.
     *
     * Through use of the {@link org.malteseduck.springframework.data.marklogic.core.mapping.Indexed} annotation you can indicate
     * use of a different type of range index for the property sorting, or specify the full path that should be used in
     * creation of the sort options.  This requires that {@link CombinedQueryDefinition#type(Class)} be called first to
     * specify the entity type.
     *
     * @param sort Sort information for the query, i.e. which properties and which orders.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition sort(Sort sort);

    /**
     * Add sorting configuration to the query.  The type that is specified will be used on all sort properties specified
     * in the {@link org.springframework.data.domain.Sort} object, and the assumption will be that how they are specified
     * in that object is exactly how the index is defined in the configuration.  For example, if you have a sort property
     * of "name" and you specify the type of IndexType.PATH then you would need a path range index defined for "name".  If the
     * property is "/pets/name" with the same index type then a path range index would need to be defined for "/pets/name".
     *
     * Usually it is better to be consistent in how you define your indexes (i.e. use either path range or element (property)
     * range indexes) so that the same type of index can be configured for many properties in a sort.
     *
     * @param sort Sort information for the query, i.e. which properties and which orders.
     * @param type The type of index that is configured for the properties.  Used to create the correct options in the
     *             combined query.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition sort(Sort sort, IndexType type);

    /**
     * Adds sorting configuration to the query.  This is the most fine-grained control of sort index configuration - you
     * can specify details for each property.  The downside would be typically you would have to iterate over a
     * {@link org.springframework.data.domain.Sort} yourself and perform logic to determine index type yourself.
     *
     * @param propertyName The name of the property, or path, or whatever (depending on index type used)
     * @param order The order, either "descending" or "ascending"
     * @param type The type of index that is configured for the properties.  Used to create the correct options in the
     *             combined query.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition sort(String propertyName, String order, IndexType type);

    /**
     * Specify which properties of a document will be included/excluded from the results.
     *
     * @see CombinedQueryDefinition#extracts(List, SelectedMode)
     */
    CombinedQueryDefinition extracts(List<String> extracts);

    /**
     * Specify which properties of a document will be included/excluded from the results.  The default is for the entire
     * document to be returned.
     *
     * Ideally you keep the mode as {@link SelectedMode#HIERARCHICAL} so that your entities will be de-serialized correctly.
     * If you create a custom entity to handle the results of just the properties flattened out then you can just use
     * the {@link SelectedMode#INCLUDE}.
     *
     * For more information see http://docs.marklogic.com/guide/java/searches#id_90087.
     *
     * @param extracts A list of XPaths that describe which properties in a document to include/exclude
     * @param mode Specify whether to include or exclude the specified properties.

     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition extracts(List<String> extracts, SelectedMode mode);

    /**
     * Limit the results that come back to the specified number.  For configuring paging of the queries it is usually
     * better to just specify your bounds in {@link org.malteseduck.springframework.data.marklogic.core.MarkLogicOperations#search(StructuredQueryDefinition, int, int, Class)}.
     *
     * @param limit Limit the results to this number of records.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition limit(int limit);

    /**
     * Specify a server transform to use on the results that are matched in the database.  This server transform will be
     * applied to each document before the set is returned from the database, so for complicated transformation logic
     * this could be more efficient than trying to do it in the Java layer.
     *
     * For more information see http://docs.marklogic.com/guide/java/transforms.
     *
     * @return The current query definition for use in continued building.
     *
     * @return
     */
    CombinedQueryDefinition transform(ServerTransform transform);

    /**
     * Add a search term to the current query.  This is used for word searches across your entire document.  For more
     * fine-grained control use {@link com.marklogic.client.query.StructuredQueryBuilder#word(StructuredQueryBuilder.TextIndex, String...)}
     * to specify specific properties or fields into which to scope the word search.
     *
     * @param qtext A search phrase.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition term(String qtext);

    /**
     * Add SPARQL queries to constraint results to only documents that have the matched triples.
     *
     * @param sparql The SPARQL query.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition sparql(String sparql);

    /**
     * Specified the entity type to use when building the query.  There are various annotations that can be used on your
     * entity classes.  If you want those to be used in making decisions on how to build certain options then make sure
     * to use this to "add" it to the query.  If not specified then the defaults will always be used.
     *
     * @param entityClass The type of the entity.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition type(Class entityClass);

    /**
     * Override the default mapping context to use when building queries.
     *
     * @param mappingContext A mapping context that maps entity information for methods that specify entity types.
     *
     * @return The current query definition for use in continued building.
     */
    CombinedQueryDefinition context(MappingContext mappingContext);

    /**
     * Indicates whether or not this combined query is a raw Query By Example query.
     *
     * @return True if the combined query contains a raw QBE.
     */
    boolean isQbe();

    /**
     * Indicates whether or not a limit has been specified on this query.
     *
     * @return True if the query is limiting.
     */
    boolean isLimiting();

    /**
     * Gets the configured limit for the current limit.
     *
     * @return The limit, or -1 if not set.
     */
    int getLimit();

    /**
     * Used by the MarkLogic Java Client Library to get the "low-level" representation of the query that can be submitted
     * to the MarkLogic REST API.
     *
     * @return JSON/XML string of a combined query.
     */
    String serialize();
}
