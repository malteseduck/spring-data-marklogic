package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.json.JSONObject;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.repository.Query;
import org.springframework.data.marklogic.repository.query.ExpressionEvaluatingParameterBinder.BindingContext;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public class StringMarkLogicQuery extends AbstractMarkLogicQuery {

    private final MarkLogicOperations operations;
    private final EvaluationContextProvider contextProvider;
    private final SpelExpressionParser parser;
    private final MarkLogicMappingContext context;
    private final String query;
    private final Query annotation;

    private static final ParameterBindingParser BINDING_PARSER = ParameterBindingParser.INSTANCE;

    private final List<ParameterBinding> queryParameterBindings;
    private final ExpressionEvaluatingParameterBinder parameterBinder;


    public StringMarkLogicQuery(MarkLogicQueryMethod method, MarkLogicOperations operations, SpelExpressionParser parser, EvaluationContextProvider contextProvider) {
        this(method.getAnnotatedQuery(), method, operations, parser, contextProvider);
    }

    public StringMarkLogicQuery(String query, MarkLogicQueryMethod method, MarkLogicOperations operations, SpelExpressionParser parser, EvaluationContextProvider contextProvider) {
        super(method, operations);
        this.context = (MarkLogicMappingContext) method.getMappingContext();
        this.annotation = method.getQueryAnnotation();
        this.operations = operations;
        this.parser = parser;
        this.contextProvider = contextProvider;

        this.queryParameterBindings = new ArrayList<>();
        this.query = BINDING_PARSER.parseAndCollectParameterBindingsFromQueryIntoBindings(query, this.queryParameterBindings);
        this.parameterBinder = new ExpressionEvaluatingParameterBinder(parser, contextProvider);
    }

    @Override
    protected StructuredQueryDefinition createQuery(ParameterAccessor accessor) {
        final Class<?> type = getQueryMethod().getEntityInformation().getJavaType();
        final MarkLogicPersistentEntity<?> entity = context.getPersistentEntity(type);

        String queryString = parameterBinder.bind(this.query, accessor, new BindingContext(getQueryMethod().getParameters(), queryParameterBindings));
        RawQueryByExampleDefinition definition = operations.executeWithClient((client, transaction) -> client.newQueryManager().newRawQueryByExampleDefinition(new StringHandle(queryString).withFormat(Format.JSON)));

        Format formatToUse = annotation.format() == Format.UNKNOWN ? entity.getDocumentFormat() : annotation.format();

        return combine()
                .type(type)
                .byExample(definition, formatToUse)
                .sort(accessor.getSort());
    }

    @Override
    protected boolean isCountQuery() {
        return false;
    }

    @Override
    protected boolean isExistsQuery() {
        return false;
    }

    @Override
    protected boolean isDeleteQuery() {
        return false;
    }


    // TODO: Copied from spring-data-mongo - what can be chopped?
    private enum ParameterBindingParser {

        INSTANCE;

        private static final String EXPRESSION_PARAM_QUOTE = "'";
        private static final String EXPRESSION_PARAM_PREFIX = "?expr";
        private static final String INDEX_BASED_EXPRESSION_PARAM_START = "?#{";
        private static final String NAME_BASED_EXPRESSION_PARAM_START = ":#{";
        private static final char CURRLY_BRACE_OPEN = '{';
        private static final char CURRLY_BRACE_CLOSE = '}';
        private static final String PARAMETER_PREFIX = "_param_";
        private static final String PARSEABLE_PARAMETER = "\"" + PARAMETER_PREFIX + "$1\"";
        private static final Pattern PARAMETER_BINDING_PATTERN = Pattern.compile("\\?(\\d+)");
        private static final Pattern PARSEABLE_BINDING_PATTERN = Pattern.compile("\"?" + PARAMETER_PREFIX + "(\\d+)\"?");

        private final static int PARAMETER_INDEX_GROUP = 1;

        public String parseAndCollectParameterBindingsFromQueryIntoBindings(String input, List<ParameterBinding> bindings) {

            if (!StringUtils.hasText(input)) {
                return input;
            }

            Assert.notNull(bindings, "Parameter bindings must not be null!");

            String transformedInput = transformQueryAndCollectExpressionParametersIntoBindings(input, bindings);
            String parseableInput = makeParameterReferencesParseable(transformedInput);

            collectParameterReferencesIntoBindings(bindings, new JSONObject(parseableInput));

            return transformedInput;
        }

        private static String transformQueryAndCollectExpressionParametersIntoBindings(String input,
                                                                                       List<ParameterBinding> bindings) {

            StringBuilder result = new StringBuilder();

            int startIndex = 0;
            int currentPos = 0;
            int exprIndex = 0;

            while (currentPos < input.length()) {

                int indexOfExpressionParameter = getIndexOfExpressionParameter(input, currentPos);

                // no expression parameter found
                if (indexOfExpressionParameter < 0) {
                    break;
                }

                int exprStart = indexOfExpressionParameter + 3;
                currentPos = exprStart;

                // eat parameter expression
                int curlyBraceOpenCnt = 1;

                while (curlyBraceOpenCnt > 0) {
                    switch (input.charAt(currentPos++)) {
                        case CURRLY_BRACE_OPEN:
                            curlyBraceOpenCnt++;
                            break;
                        case CURRLY_BRACE_CLOSE:
                            curlyBraceOpenCnt--;
                            break;
                        default:
                    }
                }

                result.append(input.subSequence(startIndex, indexOfExpressionParameter));
                result.append(EXPRESSION_PARAM_QUOTE).append(EXPRESSION_PARAM_PREFIX);
                result.append(exprIndex);
                result.append(EXPRESSION_PARAM_QUOTE);

                bindings.add(new ParameterBinding(exprIndex, true, input.substring(exprStart, currentPos - 1)));

                startIndex = currentPos;

                exprIndex++;
            }

            return result.append(input.subSequence(currentPos, input.length())).toString();
        }

        private static String makeParameterReferencesParseable(String input) {
            Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(input);
            return matcher.replaceAll(PARSEABLE_PARAMETER);
        }

        private static void collectParameterReferencesIntoBindings(List<ParameterBinding> bindings, Object value) {

            if (value instanceof String) {
                String string = ((String) value).trim();
                potentiallyAddBinding(string, bindings);

            } else if (value instanceof Pattern) {

                String string = value.toString().trim();
                Matcher valueMatcher = PARSEABLE_BINDING_PATTERN.matcher(string);

                while (valueMatcher.find()) {

                    int paramIndex = Integer.parseInt(valueMatcher.group(PARAMETER_INDEX_GROUP));
                    boolean quoted = !string.equals(PARAMETER_PREFIX + paramIndex);

                    bindings.add(new ParameterBinding(paramIndex, quoted));
                }
            } else if (value instanceof JSONObject) {
                JSONObject ob = (JSONObject) value;

                for (String field : ob.keySet()) {
                    collectParameterReferencesIntoBindings(bindings, field);
                    collectParameterReferencesIntoBindings(bindings, ob.get(field));
                }
            }
        }

        private static void potentiallyAddBinding(String source, List<ParameterBinding> bindings) {

            Matcher valueMatcher = PARSEABLE_BINDING_PATTERN.matcher(source);

            while (valueMatcher.find()) {

                int paramIndex = Integer.parseInt(valueMatcher.group(PARAMETER_INDEX_GROUP));
                boolean quoted = (source.startsWith("'") && source.endsWith("'"))
                        || (source.startsWith("\"") && source.endsWith("\""));

                bindings.add(new ParameterBinding(paramIndex, quoted));
            }
        }

        private static int getIndexOfExpressionParameter(String input, int position) {
            int indexOfExpressionParameter = input.indexOf(INDEX_BASED_EXPRESSION_PARAM_START, position);

            return indexOfExpressionParameter < 0 ? input.indexOf(NAME_BASED_EXPRESSION_PARAM_START, position)
                    : indexOfExpressionParameter;
        }
    }

    static class ParameterBinding {

        private final int parameterIndex;
        private final boolean quoted;
        private final String expression;

        /**
         * Creates a new {@link ParameterBinding} options the given {@code parameterIndex} and {@code quoted} information.
         *
         * @param parameterIndex
         * @param quoted whether or not the parameter is already quoted.
         */
        public ParameterBinding(int parameterIndex, boolean quoted) {
            this(parameterIndex, quoted, null);
        }

        public ParameterBinding(int parameterIndex, boolean quoted, String expression) {

            this.parameterIndex = parameterIndex;
            this.quoted = quoted;
            this.expression = expression;
        }

        public boolean isQuoted() {
            return quoted;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }

        public String getParameter() {
            return "?" + (isExpression() ? "expr" : "") + parameterIndex;
        }

        public String getExpression() {
            return expression;
        }

        public boolean isExpression() {
            return this.expression != null;
        }
    }
}
