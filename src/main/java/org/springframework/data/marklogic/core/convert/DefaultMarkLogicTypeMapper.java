package org.springframework.data.marklogic.core.convert;

import org.springframework.data.util.TypeInformation;

/**
 * Created by cieslinskice on 2/10/17.
 */
public class DefaultMarkLogicTypeMapper implements MarkLogicTypeMapper {

    @Override
    public TypeInformation<?> readType(MarkLogicTypeMapper markLogicTypeMapper) {
        return null;
    }

    @Override
    public <T> TypeInformation<? extends T> readType(MarkLogicTypeMapper markLogicTypeMapper, TypeInformation<T> typeInformation) {
        return null;
    }

    @Override
    public void writeType(Class<?> aClass, MarkLogicTypeMapper markLogicTypeMapper) {

    }

    @Override
    public void writeType(TypeInformation<?> typeInformation, MarkLogicTypeMapper markLogicTypeMapper) {

    }
}
