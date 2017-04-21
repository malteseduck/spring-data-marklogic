/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.marklogic.repository.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.data.marklogic.repository.query.StringMarkLogicQuery.ParameterBinding;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Copied from spring-data-mongo - what can be chopped?
class ExpressionEvaluatingParameterBinder {

	private final SpelExpressionParser expressionParser;
	private final EvaluationContextProvider evaluationContextProvider;
	private final ObjectMapper mapper = new ObjectMapper();

	public ExpressionEvaluatingParameterBinder(SpelExpressionParser expressionParser,
			EvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(expressionParser, "ExpressionParser must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		this.expressionParser = expressionParser;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	public String bind(String raw, ParameterAccessor accessor, BindingContext bindingContext) {

		if (!StringUtils.hasText(raw)) {
			return null;
		}

		return replacePlaceholders(raw, accessor, bindingContext);
	}

	/**
	 * Replaced the parameter placeholders with the actual parameter values from the given {@link ParameterBinding}s.
	 * 
	 * @param input must not be {@literal null} or empty.
	 * @param accessor must not be {@literal null}.
	 * @param bindingContext must not be {@literal null}.
	 * @return
	 */
	private String replacePlaceholders(String input, ParameterAccessor accessor, BindingContext bindingContext) {

		if (!bindingContext.hasBindings()) {
			return input;
		}

		if (input.matches("^\\?\\d+$")) {
			return getParameterValueForBinding(accessor, bindingContext.getParameters(), bindingContext.getBindings().iterator().next());
		}

		Matcher matcher = createReplacementPattern(bindingContext.getBindings()).matcher(input);
		StringBuffer buffer = new StringBuffer();

		int parameterIndex = 0;
		while (matcher.find()) {

			Placeholder placeholder = extractPlaceholder(parameterIndex++, matcher);
			ParameterBinding binding = bindingContext.getBindingFor(placeholder);
			String valueForBinding = getParameterValueForBinding(accessor, bindingContext.getParameters(), binding);

			// appendReplacement does not like unescaped $ sign and others, so we need to quote that stuff first
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(valueForBinding));
			if (StringUtils.hasText(placeholder.getSuffix())) {
				buffer.append(placeholder.getSuffix());
			}

			if (binding.isQuoted() || placeholder.isQuoted()) {
				postProcessQuotedBinding(buffer, valueForBinding,
						!binding.isExpression() ? accessor.getBindableValue(binding.getParameterIndex()) : null,
						binding.isExpression());
			}
		}

		matcher.appendTail(buffer);
		return buffer.toString();
	}

	/**
	 * Sanitize String binding by replacing single quoted values with double quotes which prevents potential single quotes
	 * contained in replacement to interfere with the Json parsing. Also take care of complex objects by removing the
	 * quotation entirely.
	 *
	 * @param buffer the {@link StringBuffer} to operate upon.
	 * @param valueForBinding the actual binding value.
	 * @param raw the raw binding value
	 * @param isExpression {@literal true} if the binding value results from a SpEL expression.
	 */
	private void postProcessQuotedBinding(StringBuffer buffer, String valueForBinding, Object raw, boolean isExpression) {

		int quotationMarkIndex = buffer.length() - valueForBinding.length() - 1;
		char quotationMark = buffer.charAt(quotationMarkIndex);

		while (quotationMark != '\'' && quotationMark != '"') {

			quotationMarkIndex--;

			if (quotationMarkIndex < 0) {
				throw new IllegalArgumentException("Could not find opening quotes for quoted parameter");
			}

			quotationMark = buffer.charAt(quotationMarkIndex);
		}

		// remove quotation char before the complex object string
		if (valueForBinding.startsWith("{") && (raw instanceof Object || isExpression)) {

			buffer.deleteCharAt(quotationMarkIndex);

		} else {

			if (quotationMark == '\'') {
				buffer.replace(quotationMarkIndex, quotationMarkIndex + 1, "\"");
			}

			buffer.append("\"");
		}
	}

	private String getParameterValueForBinding(ParameterAccessor accessor, Parameters parameters, ParameterBinding binding) {

		Object value = binding.isExpression()
				? evaluateExpression(binding.getExpression(), parameters, null)
				: accessor.getBindableValue(binding.getParameterIndex());

		if (value instanceof String && binding.isQuoted()) {

			if (binding.isExpression() && ((String) value).startsWith("{")) {
				return (String) value;
			}

			return QuotedString.unquote(JSONObject.quote((String) value));
		}

		try {
			return mapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return String.valueOf(value);
		}
	}

	/**
	 * Evaluates the given {@code expressionString}.
	 *
	 * @param expressionString must not be {@literal null} or empty.
	 * @param parameters must not be {@literal null}.
	 * @param parameterValues must not be {@literal null}.
	 * @return
	 */
	private Object evaluateExpression(String expressionString, Parameters parameters, Object[] parameterValues) {

		EvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(parameters, parameterValues);
		Expression expression = expressionParser.parseExpression(expressionString);

		return expression.getValue(evaluationContext, Object.class);
	}

	/**
	 * Creates a replacement {@link Pattern} for all {@link ParameterBinding#getParameter() binding parameters} including
	 * a potentially trailing quotation mark.
	 *
	 * @param bindings
	 * @return
	 */
	private Pattern createReplacementPattern(List<ParameterBinding> bindings) {

		StringBuilder regex = new StringBuilder();

		for (ParameterBinding binding : bindings) {

			regex.append("|");
			regex.append("(" + Pattern.quote(binding.getParameter()) + ")");
			regex.append("(\\W?['\"])?"); // potential quotation char (as in { foo : '?0' }).
		}

		return Pattern.compile(regex.substring(1));
	}

	/**
	 * Extract the placeholder stripping any trailing trailing quotation mark that might have resulted from the
	 * {@link #createReplacementPattern(List) pattern} used.
	 *
	 * @param parameterIndex The actual parameter index.
	 * @param matcher The actual {@link Matcher}.
	 * @return
	 */
	private Placeholder extractPlaceholder(int parameterIndex, Matcher matcher) {

		String rawPlaceholder = matcher.group(parameterIndex * 3 + 1);
		String suffix = matcher.group(parameterIndex * 3 + 2);

		if (!StringUtils.hasText(rawPlaceholder)) {

			rawPlaceholder = matcher.group();
			if (rawPlaceholder.matches(".*\\d$")) {
				suffix = "";
			} else {
				int index = rawPlaceholder.replaceAll("[^\\?0-9]*$", "").length() - 1;
				if (index > 0 && rawPlaceholder.length() > index) {
					suffix = rawPlaceholder.substring(index + 1);
				}
			}
			if (QuotedString.endsWithQuote(rawPlaceholder)) {
				rawPlaceholder = rawPlaceholder.substring(0, rawPlaceholder.length() - (StringUtils.hasText(suffix) ? suffix.length() : 1));
			}
		}

		if (StringUtils.hasText(suffix)) {
			boolean quoted = QuotedString.endsWithQuote(suffix);
			return new Placeholder(parameterIndex, rawPlaceholder, quoted, quoted ? QuotedString.unquoteSuffix(suffix) : suffix);
		}
		return new Placeholder(parameterIndex, rawPlaceholder, false, null);
	}

	/**
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 * @since 1.9
	 */
	static class BindingContext {

		final Parameters parameters;
		final Map<Placeholder, StringMarkLogicQuery.ParameterBinding> bindings;

		/**
		 * Creates new {@link BindingContext}.
		 *
		 * @param parameters
		 * @param bindings
		 */
		public BindingContext(Parameters parameters, List<StringMarkLogicQuery.ParameterBinding> bindings) {

			this.parameters = parameters;
			this.bindings = mapBindings(bindings);
		}

		/**
		 * @return {@literal true} when list of bindings is not empty.
		 */
		boolean hasBindings() {
			return !CollectionUtils.isEmpty(bindings);
		}

		/**
		 * Get unmodifiable list of {@link ParameterBinding}s.
		 *
		 * @return never {@literal null}.
		 */
		public List<ParameterBinding> getBindings() {
			return new ArrayList<ParameterBinding>(bindings.values());
		}

		/**
		 * Get the concrete {@link ParameterBinding} for a given {@literal placeholder}.
		 *
		 * @param placeholder must not be {@literal null}.
		 * @return
		 * @throws java.util.NoSuchElementException
		 * @since 1.10
		 */
		ParameterBinding getBindingFor(Placeholder placeholder) {

			if (!bindings.containsKey(placeholder)) {
				throw new NoSuchElementException(String.format("Could not to find binding for placeholder '%s'.", placeholder));
			}

			return bindings.get(placeholder);
		}

		public Parameters getParameters() {
			return parameters;
		}

		private static Map<Placeholder, StringMarkLogicQuery.ParameterBinding> mapBindings(List<ParameterBinding> bindings) {

			Map<Placeholder, StringMarkLogicQuery.ParameterBinding> map = new LinkedHashMap<>(bindings.size(), 1);

			int parameterIndex = 0;
			for (ParameterBinding binding : bindings) {
				map.put(new Placeholder(parameterIndex++, binding.getParameter(), binding.isQuoted(), null), binding);
			}

			return map;
		}
	}

	static class Placeholder {

		private final int parameterIndex;
		private final String parameter;
		private final boolean quoted;
		private final String suffix;

		public Placeholder(int parameterIndex, String parameter, boolean quoted, String suffix) {
			this.parameterIndex = parameterIndex;
			this.parameter = parameter;
			this.quoted = quoted;
			this.suffix = suffix;
		}

		@Override
		public String toString() {
			return quoted ? String.format("'%s'", parameter + (suffix != null ? suffix : ""))
					: parameter + (suffix != null ? suffix : "");
		}

		public int getParameterIndex() {
			return parameterIndex;
		}

		public String getParameter() {
			return parameter;
		}

		public boolean isQuoted() {
			return quoted;
		}

		public String getSuffix() {
			return suffix;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Placeholder that = (Placeholder) o;

			if (parameterIndex != that.parameterIndex) return false;
			return parameter != null ? parameter.equals(that.parameter) : that.parameter == null;
		}

		@Override
		public int hashCode() {
			int result = parameterIndex;
			result = 31 * result + (parameter != null ? parameter.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Utility to handle quoted strings using single/double quotes.
	 *
	 * @author Mark Paluch
	 */
	static class QuotedString {

		/**
		 * @param string
		 * @return {@literal true} if {@literal string} ends with a single/double quote.
		 */
		public static boolean endsWithQuote(String string) {
			return string.endsWith("'") || string.endsWith("\"");
		}

		/**
		 * Remove trailing quoting from {@literal quoted}.
		 *
		 * @param quoted
		 * @return {@literal quoted} with removed quotes.
		 */
		public static String unquoteSuffix(String quoted) {
			return quoted.substring(0, quoted.length() - 1);
		}

		/**
		 * Remove leading and trailing quoting from {@literal quoted}.
		 *
		 * @param quoted
		 * @return {@literal quoted} with removed quotes.
		 */
		public static String unquote(String quoted) {
			return quoted.substring(1, quoted.length() - 1);
		}
	}
}
