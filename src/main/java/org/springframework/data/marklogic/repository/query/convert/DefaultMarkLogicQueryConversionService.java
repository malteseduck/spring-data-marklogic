package org.springframework.data.marklogic.repository.query.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryBuilder.ContainerIndex;
import com.marklogic.client.query.StructuredQueryBuilder.RangeIndex;
import com.marklogic.client.query.StructuredQueryBuilder.TextIndex;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.marklogic.repository.query.QueryType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.time.*;
import java.util.*;

public class DefaultMarkLogicQueryConversionService implements QueryConversionService {

    private final Map<Object, QueryTypeConverter> converters;
    private static final ObjectMapper m = new ObjectMapper();
    private final static StructuredQueryBuilder qb = new StructuredQueryBuilder();

    public DefaultMarkLogicQueryConversionService() {
        this(new LinkedHashMap<>());
    }

    public DefaultMarkLogicQueryConversionService(Map<Object, QueryTypeConverter> converters) {

        Assert.notNull(converters, "List of converters must not be null!");

        Map<Object, QueryTypeConverter> toRegister = new LinkedHashMap<>();

        // Add user provided converters to make sure they can override the defaults
        toRegister.putAll(converters);
        toRegister.putAll(getConvertersToRegister());

        this.converters = Collections.unmodifiableMap(toRegister);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
        return convert(index, source, options, TypeDescriptor.forObject(source));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options, TypeDescriptor sourceType) {
        if (sourceType == null) {
            Assert.isTrue(source == null, "Source must be [null] if source type == [null]");
            return convertNullSource(index, options);
        }
        if (source != null && !sourceType.getObjectType().isInstance(source)) {
            throw new IllegalArgumentException("Source to convert from must be an instance of [" +
                    sourceType + "]; instead it was a [" + source.getClass().getName() + "]");
        }

        sourceType = getTypeFromSource(source, sourceType);

        QueryTypeConverter converter = getConverter(sourceType);
        if (converter != null) {
            if (!converter.supports(index)) {
                throw new IllegalArgumentException("Type of [" + sourceType + "] is not supported by [" + converter.getClass().getName() + "]");
            }
            return converter.convert(index, source, options, this);
        }

        throw new IllegalArgumentException("Unable to convert type of [" + sourceType + "] to structured query definition");
    }

    public Map<Object, QueryTypeConverter> getConvertersToRegister() {

        Map<Object, QueryTypeConverter> converters = new HashMap<>();

        // TODO: Can we just register the class and not worry if it is an array?
        converters.put(Instant.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(LocalDateTime.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(ZonedDateTime.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(ZoneId.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(Duration.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(Period.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(Calendar.class, OldDateToStringValueConverter.INSTANCE);
        converters.put(Date.class, OldDateToStringValueConverter.INSTANCE);
        converters.put(Number.class, NumberToValueConverter.INSTANCE);
        converters.put(String.class, ObjectToStringValueConverter.INSTANCE);
        converters.put(Boolean.class, BooleanToValueConverter.INSTANCE);
        converters.put(Object.class, ObjectToContainerQueryConverter.INSTANCE);

        converters.put(Map.class, MapToContainerQueryConverter.INSTANCE);
        converters.put(LinkedHashMap.class, MapToContainerQueryConverter.INSTANCE);

        return converters;
    }

    protected TypeDescriptor getTypeFromSource(Object source, TypeDescriptor sourceType) {
        if (source != null) {
            if (source instanceof Collection || source.getClass().isArray()) {
                if (source instanceof Collection && !((Collection) source).isEmpty()) {
                    sourceType = TypeDescriptor.forObject(((Collection) source).iterator().next());
                } else if (source.getClass().isArray() && ((Object[]) source).length > 0) {
                    sourceType = TypeDescriptor.forObject(((Object[]) source)[0]);
                }
            }
        }

        if (sourceType != null) {
            return sourceType;
        } else {
            return TypeDescriptor.forObject(source);
        }
    }

    protected StructuredQueryDefinition convertNullSource(PropertyIndex index, List<String> options) {
        return ObjectToStringValueConverter.INSTANCE.convert(index, null, options);
    }

    protected QueryTypeConverter getConverter(TypeDescriptor sourceType) {
        QueryTypeConverter converter = this.converters.get(sourceType.getObjectType());

        // TODO: Use the class hierarchy as candidates for looking up the converter
        // TODO: Use the package structure for candidates as well? i.e. all of "java.time" should use the "string" converter.
        if (converter != null) return converter;

        // Without the hierarchy added to the converter map we can check some groups
        if (Boolean.class.isAssignableFrom(sourceType.getObjectType())) return BooleanToValueConverter.INSTANCE;
        if (Number.class.isAssignableFrom(sourceType.getObjectType())) return NumberToValueConverter.INSTANCE;
        if (String.class.isAssignableFrom(sourceType.getObjectType())) return ObjectToStringValueConverter.INSTANCE;
        return getDefaultConverter();
    }

    protected QueryTypeConverter getDefaultConverter() {
        return ObjectToContainerQueryConverter.INSTANCE;
    }

    public enum NumberToValueConverter implements QueryTypeConverter<Object> {
        INSTANCE;

        @Override
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
            return index.getType() == QueryType.RANGE
                    ? rangeQuery(index, options, source)
                    : numberValueQuery(index, options, source);
        }

        @Override
        public boolean supports(PropertyIndex index) {
            return index.get() instanceof TextIndex || index.get() instanceof RangeIndex;
        }
    }

    public enum BooleanToValueConverter implements QueryTypeConverter<Object> {
        INSTANCE;

        @Override
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
            return index.getType() == QueryType.RANGE
                    ? rangeQuery(index, options, source)
                    : booleanValueQuery(index, options, source);
        }

        @Override
        public boolean supports(PropertyIndex index) {
            return index.getType() == QueryType.VALUE || index.getType() == QueryType.RANGE;
        }
    }

    public enum OldDateToStringValueConverter implements  QueryTypeConverter<Object> {
        INSTANCE;

        @Override
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
            Instant instant;
            if (source instanceof Calendar) instant = ((Calendar)source).toInstant();
            else if (source instanceof Date) instant = ((Date)source).toInstant();
            else instant = Instant.parse(source.toString());
            return index.getType() == QueryType.RANGE
                    ? rangeQuery(index, options, instant)
                    : stringValueQuery(index, options, instant);
        }

        @Override
        public boolean supports(PropertyIndex index) {
            return index.getType() == QueryType.VALUE || index.getType() == QueryType.RANGE;
        }
    }

    public enum ObjectToStringValueConverter implements QueryTypeConverter<Object> {
        INSTANCE;

        @Override
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
            return index.getType() == QueryType.RANGE
                    ? rangeQuery(index, options, source)
                    : stringValueQuery(index, options, source);
        }

        @Override
        public boolean supports(PropertyIndex index) {
            return index.getType() == QueryType.VALUE || index.getType() == QueryType.RANGE;
        }
    }

    public enum ObjectToContainerQueryConverter implements QueryTypeConverter<Object> {
        INSTANCE;

        @Override
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
            return convert(index, source, options, null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options, QueryConversionService service) {
            Map<String, Object> props = (Map<String, Object>) m.convertValue(source, Map.class);
            return MapToContainerQueryConverter.INSTANCE.convert(index, props, options, service);
        }
    }

    public enum MapToContainerQueryConverter implements QueryTypeConverter<Object> {
        INSTANCE;

        @Override
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options) {
            return convert(index, source, options, null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public StructuredQueryDefinition convert(PropertyIndex index, Object source, List<String> options, QueryConversionService service) {
            return qb.containerQuery((ContainerIndex) index.get(), convert(index, (Map<String, Object>) source, service));
        }

        protected StructuredQueryDefinition convert(PropertyIndex index, Map<String, Object> props, QueryConversionService service) {
            if (!props.isEmpty() && service != null) {
                return qb.and(
                        props.entrySet().stream()
                                .map(entry -> service.convert(index.child(entry.getKey()), entry.getValue(), null))
                                .toArray(StructuredQueryDefinition[]::new)
                );
            } else {
                return qb.and();
            }
        }
    }

    private static StructuredQueryDefinition booleanValueQuery(PropertyIndex index, List<String> options, Object values) {
        if (options != null && !options.isEmpty())
            return qb.value((TextIndex) index.get(), null, options.toArray(new String[0]), 1.0, as(values, boolean.class));
        else
            return qb.value((TextIndex) index.get(), as(values, boolean.class));

    }

    private static StructuredQueryDefinition numberValueQuery(PropertyIndex index, List<String> options, Object values) {
        if (options != null && !options.isEmpty())
            return qb.value((TextIndex) index.get(), null, options.toArray(new String[0]), 1.0, asArray(values, Number[].class));
        else
            return qb.value((TextIndex) index.get(), asArray(values, Number[].class));

    }

    private static StructuredQueryDefinition stringValueQuery(PropertyIndex index, List<String> options, Object values) {
        if (options != null && !options.isEmpty())
            return qb.value((TextIndex) index.get(), null, options.toArray(new String[0]), 1.0, asArray(values, String[].class));
        else
            return qb.value((TextIndex) index.get(), asArray(values, String[].class));
    }

    private static StructuredQueryDefinition rangeQuery(PropertyIndex index, List<String> options, Object values) {
        if (options != null && !options.isEmpty())
            return qb.range((RangeIndex) index.get(), index.getRangeIndexType(), options.toArray(new String[0]), index.getOperator(), asArray(values, Object[].class));
        else
            return qb.range((RangeIndex) index.get(), index.getRangeIndexType(), index.getOperator(), asArray(values, Object[].class));
    }

    @SuppressWarnings("unchecked")
    private static <T> T as(Object value, Class<T> type) {
        if (value instanceof Collection ||
                value != null && (value.getClass().isArray() || !ClassUtils.isAssignable(type, value.getClass()))) {
            throw new IllegalArgumentException(
                    String.format("Expected parameter type of %s but got %s!", type, value.getClass()));
        }

        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] asArray(Object values, Class<T[]> type) {
        if (values instanceof Collection) {
            return (T[]) ((Collection<T>) values).toArray();
        } else if (values != null && values.getClass().isArray()) {
            return (T[]) values;
        } else if (values != null && String[].class.equals(type)){
            return Arrays.copyOf(new Object[] { values.toString() }, 1, type);
        } else {
            return Arrays.copyOf(new Object[] { values }, 1, type);
        }
    }
}
