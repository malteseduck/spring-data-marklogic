package org.malteseduck.springframework.data.marklogic.core.convert;

import com.marklogic.client.document.ServerTransform;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.repository.query.ParameterAccessor;

/**
 * Interface for class that creates a configured server transform for read and write operations.  The default functionality
 * for server transforms is that there are none, so all the methods ultimately default to returning null.  Any of the
 * methods needed may be implemented to change that.
 *
 * Implemented the methods that take the entity type and the params to make a default for all read or write operations.
 *
 * If your implementation class has a constructor with a param of type {@link MarkLogicConverter} then the instantiated
 * converter instance will be injected in when your class is instantiated.  If you don't need the converter then
 */
public interface ServerTransformer {

    /**
     * Global read transform.  All template read operations use this transform, unless otherwise overridden in a
     * function call in a custom repository implementation.
     *
     * When the repository query builder is creating MarkLogic queries from your repository functions it will pass in
     * the parameters passed into the method.  You can perform any business logic and add the values to the
     * {@link ServerTransform} object as well, if necessary.
     *
     * The entity configuration will always be passed - this can be used to inspect annotations or do other logic to get
     * other information about the entity class
     *
     * @param pEntity The persistent entity configuration class which can be used to get information about the entity
     * @param params Function parameters passed to a repository query function.  Perform business logic and merge them
     *               into the {@link ServerTransform} parameters, if necessary.
     */
    default <T> ServerTransform reader(MarkLogicPersistentEntity<T> pEntity, ParameterAccessor params) {
        return null;
    }

    /**
     * Global write transform.  All template write operations use this transform, unless otherwise overridden in a
     * function call in a custom repository implementation.
     *
     * @param pEntity The persistent entity configuration class which can be used to get information about the entity
     */
    default <T> ServerTransform writer(MarkLogicPersistentEntity<T> pEntity) {
        return null;
    }
}
