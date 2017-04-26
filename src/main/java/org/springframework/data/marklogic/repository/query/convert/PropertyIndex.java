package org.springframework.data.marklogic.repository.query.convert;

import com.marklogic.client.io.Format;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryBuilder.Operator;
import com.marklogic.client.query.StructuredQueryBuilder.TextIndex;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.marklogic.repository.query.QueryType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

public class PropertyIndex {

    private final Object index;
    private final QueryType type;
    private MarkLogicPersistentProperty property;
    private MarkLogicPersistentEntity entity;
    private Format format;
    private Operator operator;
    private final static StructuredQueryBuilder qb = new StructuredQueryBuilder();


    public PropertyIndex(Object index, QueryType type) {
        this.index = index;
        this.type = type;
    }

    public Object get() {
        return index;
    }

    public Format getFormat() {
        return this.format;
    }

    public QueryType getType() {
        return type;
    }

    public PropertyIndex withProperty(MarkLogicPersistentProperty property) {
        this.property = property;
        this.entity = (MarkLogicPersistentEntity) property.getOwner();
        this.format = entity.getDocumentFormat();
        return this;
    }

    public PropertyIndex child(String name) {
        TextIndex index = getFormat() == Format.XML
                ? qb.element(name)
                : qb.jsonProperty(name);
        return new PropertyIndex(index, getType())
                .withFormat(getFormat());
    }

    public PropertyIndex withOperator(Operator operator) {
        this.operator = operator;
        return this;
    }

    public PropertyIndex withFormat(Format format) {
        this.format = format;
        return this;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getRangeIndexType() {
        if (type != QueryType.RANGE || property == null) {
            throw new IllegalArgumentException("Index " + index + " is not a properly formed range index type");
        }
        String type;
        Class<?> propertyClass = property.getActualType();
        if (String.class.isAssignableFrom(propertyClass)) {
            type = "xs:string";
        } else if (Integer.TYPE.equals(propertyClass)) {
            type = "xs:int";
        } else if (Integer.class.isAssignableFrom(propertyClass)) {
            type = "xs:int";
        } else if (Long.TYPE.equals(propertyClass)) {
            type = "xs:long";
        } else if (Long.class.isAssignableFrom(propertyClass)) {
            type = "xs:long";
        } else if (Float.TYPE.equals(propertyClass)) {
            type = "xs:float";
        } else if (Float.class.isAssignableFrom(propertyClass)) {
            type = "xs:float";
        } else if (Double.TYPE.equals(propertyClass)) {
            type = "xs:double";
        } else if (Double.class.isAssignableFrom(propertyClass)) {
            type = "xs:double";
        } else if (Number.class.isAssignableFrom(propertyClass)) {
            type = "xs:decimal";
        } else if (Date.class.isAssignableFrom(propertyClass) ||
                Calendar.class.isAssignableFrom(propertyClass) ||
                Instant.class.isAssignableFrom(propertyClass) ||
                LocalDateTime.class.isAssignableFrom(propertyClass) ||
                ZonedDateTime.class.isAssignableFrom(propertyClass)) {
            type = "xs:dateTime";
        } else {
            throw new IllegalArgumentException("Property " + property.getName() + " is not a native Java type");
        }
        return type;
    }
}
