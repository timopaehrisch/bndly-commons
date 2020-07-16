package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.vendor.ErrorCodeMapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultErrorCodeMapper implements ErrorCodeMapper {

	@Override
	public final SchemaException map(SQLException exception, Engine engine) {
		return map(null, exception, exception.getErrorCode(), engine);
	}

	@Override
	public final SchemaException map(int errorCode, Engine engine) {
		return map(null, null, errorCode, engine);
	}

	@Override
	public final SchemaException map(String message, SQLException exception, Engine engine) {
		return map(message, exception, exception.getErrorCode(), engine);
	}

	@Override
	public final SchemaException map(String message, int errorCode, Engine engine) {
		return map(message, null, errorCode, engine);
	}

	protected SchemaException map(String message, SQLException exception, int errorCode, Engine engine) {
		if (message != null) {
			if (exception != null) {
				return new SchemaException(message, exception);
			} else {
				return new SchemaException(message);
			}
		} else {
			if (exception != null) {
				return new SchemaException("error code: " + errorCode, exception);
			} else {
				return new SchemaException("error code: " + errorCode);
			}
		}
	}

		
	protected final SchemaException createSchemaException(Class<? extends SchemaException> exceptionClass, SQLException ex, String defaultMessage, String message) {
		boolean hasCause = ex != null;
		Constructor<SchemaException> constructor;
		if (hasCause) {
			try {
				constructor = (Constructor<SchemaException>) exceptionClass.getConstructor(String.class, Throwable.class);
			} catch (NoSuchMethodException | SecurityException ex1) {
				return new SchemaException("failed to find constructor. original message: " + defaultMessage + "; " + message, ex);
			}
		} else {
			try {
				constructor = (Constructor<SchemaException>) exceptionClass.getConstructor(String.class);
			} catch (NoSuchMethodException | SecurityException ex1) {
				return new SchemaException("failed to find constructor. original message: " + defaultMessage + "; " + message);
			}
		}
		SchemaException mappedExcpetion;
		try {
			if (hasCause) {
				mappedExcpetion = constructor.newInstance(message, ex);
			} else {
				mappedExcpetion = constructor.newInstance(message);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex1) {
			if (hasCause) {
				return new SchemaException("failed to invoke constructor. original message: " + defaultMessage + "; " + message, ex1);
			} else {
				return new SchemaException("failed to invoke constructor. original message: " + defaultMessage + "; " + message);
			}
		}
		return mappedExcpetion;
	}

}
