package org.bndly.schema.api;

/*-
 * #%L
 * Schema API
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
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
 * #L%
 */

import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.ValueProvider;

public final class ValueUtil {

	private ValueUtil() {
	}

	public static void appendToArgs(QueryRenderContext ctx, Object value) {
		ctx.getArgs().add(unwrapValue(value));
	}

	/**
	 * Unwraps the passed in value object, if it is a {@link ValueProvider} instance.
	 * Otherwise the value will be returned as it is.
	 * @param value the value to unwrap
	 * @return an unwrapped {@link ValueProvider} or the original <code>value</code>
	 */
	public static Object unwrapValue(Object value) {
		if (ValueProvider.class.isInstance(value)) {
			value = ((ValueProvider) value).get();
		}
		return value;
	}
}
