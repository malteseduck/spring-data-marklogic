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
package org.springframework.data.marklogic.repository.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;

import java.util.Arrays;
import java.util.Iterator;

class StubParameterAccessor implements ParameterAccessor {

	private final Object[] values;

	public static ParameterAccessor getAccessor(Object... parameters) {
		return new StubParameterAccessor(parameters);
	}

	@SuppressWarnings("unchecked")
	public StubParameterAccessor(Object... values) {
		this.values = values;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getPageable()
	 */
	public Pageable getPageable() {
		return null;
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
		return null;
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
	public Class<?> getDynamicProjection() {
		return null;
	}
}
