/*
 * Copyright 2011-2015 the original author or authors.
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
package io.github.malteseduck.springframework.data.marklogic.repository.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

class StubParameterAccessor implements ParameterAccessor {

	private final Object[] values;
	private Pageable pageable;
	private Sort sort;

	public static ParameterAccessor getAccessor(Object... parameters) {
		return new StubParameterAccessor(parameters);
	}

	@SuppressWarnings("unchecked")
	public StubParameterAccessor(Object... values) {
		this.values = values;

		if (values != null) {
			Pageable pageable =
					(Pageable) Arrays.stream(values)
							.filter(value -> value instanceof Pageable)
							.findFirst().orElse(null);

			if (pageable != null) sort = pageable.getSort();
			else
				sort =
						(Sort) Arrays.stream(values)
								.filter(value -> value instanceof Sort)
								.findFirst().orElse(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getPageable()
	 */
	public Pageable getPageable() {
		return pageable;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getBindableValue(int)
	 */
	public Object getBindableValue(int index) {
		return values[index];
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#hasBindableNullValue()
	 */
	public boolean hasBindableNullValue() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getSort()
	 */
	public Sort getSort() {
		return sort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#iterator()
	 */
	public Iterator<Object> iterator() {
		if (values == null)
			return Arrays.asList(new Object[]{ null }).iterator();
		else
			return Arrays.asList(values).iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getDynamicProjection()
	 */
	@Override
	public Optional<Class<?>> getDynamicProjection() {
		return Optional.empty();
	}

    @Override
    public Class<?> findDynamicProjection() {
        return null;
    }
}
