package org.springframework.data.marklogic.core;

import com.marklogic.client.MarkLogicServerException;
import org.springframework.dao.*;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

public class MarkLogicExceptionTranslator implements PersistenceExceptionTranslator {

    private static final Set<String> DULICATE_KEY_EXCEPTIONS = new HashSet<String>();

    private static final Set<String> RESOURCE_FAILURE_EXCEPTIONS = new HashSet<String>();

    private static final Set<String> RESOURCE_USAGE_EXCEPTIONS = new HashSet<String>();

    private static final Set<String> DATA_INTEGRETY_EXCEPTIONS = new HashSet<String>();

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

        String exception = ClassUtils.getShortName(ClassUtils.getUserClass(ex.getClass()));

        if (DULICATE_KEY_EXCEPTIONS.contains(exception)) {
            return new DuplicateKeyException(ex.getMessage(), ex);
        }

        if (RESOURCE_FAILURE_EXCEPTIONS.contains(exception)) {
            return new DataAccessResourceFailureException(ex.getMessage(), ex);
        }

        if (RESOURCE_USAGE_EXCEPTIONS.contains(exception)) {
            return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
        }

        if (DATA_INTEGRETY_EXCEPTIONS.contains(exception)) {
            return new DataIntegrityViolationException(ex.getMessage(), ex);
        }

        if (ex instanceof MarkLogicServerException) {
        }

        return null;
    }
}
