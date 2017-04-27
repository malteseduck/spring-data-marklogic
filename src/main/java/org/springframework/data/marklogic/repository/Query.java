package org.springframework.data.marklogic.repository;

import com.marklogic.client.io.Format;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.marklogic.repository.query.QueryType;
import org.springframework.data.marklogic.repository.query.SelectedMode;

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
     * Sometimes it is better to force the use of a range index for equality checks (to point to specific properties instead of
     * any property in a hierarchy).  This allows the ability to do so.  Changing to the RANGE type would require a range index
     * created, and possible the @Indexed attribute with the path set on the property in the POJO class.
     *
     * @return
     */
    QueryType type() default QueryType.VALUE;

    /**
     * The document format to match, either XML or JSON.  Currently can't match a mix using this approach, you would need
     * a StructuredQueryDefinition using the query builder in order to match both formats
     * @return
     */
    Format format() default Format.UNKNOWN;

    /**
     * Defines the properties that should be returned for the given query. Note that only these properties will make it into the
     * domain object returned.
     *
     * @return
     */
    String[] extract() default {};

    /**
     * Used in conjunction with the extract paths.  This determines how the extracted nodes are returned.  By default this
     * returns specified nodes in their original hierarchy, but you can also specify to just return the extracted nodes, or to
     * exclude the specified nodes.
     * @return
     */
    SelectedMode selected() default SelectedMode.HIERARCHICAL;

    /**
     * To specify any query options to use in the query.  Since the query could end up as a range query or a text query of some
     * kind, or MarkLogic could add additional options, this is not limited by an enumeration but the query will fail with
     * incorrect options.
     *
     * @return
     */
    String[] options() default {};
}
