package org.bndly.rest.client.impl.hateoas;

/*-
 * #%L
 * REST Client Impl
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

import org.bndly.rest.atomlink.api.annotation.ErrorBean;
import org.bndly.rest.atomlink.api.annotation.ErrorKeyValuePair;
import org.bndly.rest.atomlink.api.annotation.StacktraceElementBean;
import org.bndly.rest.client.api.ExceptionThrower;
import org.bndly.rest.client.exception.RemoteCause;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ContextImpl implements ExceptionThrower.Context {
	private final ErrorBean errorBean;
	private final RemoteCause cause;
	private final int statusCode;
	private final String httpMethod;
	private final String url;
	
	private final Map<String, String[]> errorBeanStringArrayValues = new HashMap<>();
	private final Map<String, String> errorBeanStringValues = new HashMap<>();
	private final Map<String, Long> errorBeanLongValues = new HashMap<>();

	public ContextImpl(ErrorBean errorBean, int statusCode, String httpMethod, String url) {
		this.errorBean = errorBean;
		this.cause = extractCause(errorBean);
		this.statusCode = statusCode;
		this.httpMethod = httpMethod;
		this.url = url;
	}
	
	
	@Override
	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public String getHttpMethod() {
		return httpMethod;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public RemoteCause getCause() {
		return cause;
	}

	@Override
	public ErrorBean getErrorBean() {
		return errorBean;
	}

	@Override
	public String getErrorBeanStringValue(String key) {
		if (errorBean == null) {
			return null;
		}
		if (errorBeanStringValues.containsKey(key)) {
			return errorBeanStringValues.get(key);
		} else {
			String value = getStringValue(key, errorBean);
			errorBeanStringValues.put(key, value);
			return value;
		}
	}

	@Override
	public String[] getErrorBeanStringValues(String key) {
		if (errorBean == null) {
			return null;
		}
		if (errorBeanStringArrayValues.containsKey(key)) {
			return errorBeanStringArrayValues.get(key);
		} else {
			String[] values = getStringValues(key, errorBean);
			errorBeanStringArrayValues.put(key, values);
			return values;
		}
	}
	
	@Override
	public Long getErrorBeanLongValue(String key) {
		if (errorBean == null) {
			return null;
		}
		if (errorBeanLongValues.containsKey(key)) {
			return errorBeanLongValues.get(key);
		} else {
			Long value = getLongValue(key, errorBean);
			errorBeanLongValues.put(key, value);
			return value;
		}
	}
	
	private String[] getStringValues(String key, ErrorBean e) {
		ArrayList<String> r = new ArrayList<>();
		List<? extends ErrorKeyValuePair> desc = e.getDescription();
		if (desc != null) {
			for (ErrorKeyValuePair errorKeyValuePair : desc) {
				if (key.equals(errorKeyValuePair.getKey())) {
					if (errorKeyValuePair.getStringValue() != null) {
						r.add(errorKeyValuePair.getStringValue());
					}
				}
			}
		}
		return r.toArray(new String[r.size()]);
	}
	
	private String getStringValue(String key, ErrorBean e) {
		List<? extends ErrorKeyValuePair> desc = e.getDescription();
		if (desc != null) {
			for (ErrorKeyValuePair errorKeyValuePair : desc) {
				if (key.equals(errorKeyValuePair.getKey())) {
					return errorKeyValuePair.getStringValue();
				}
			}
		}
		return null;
	}

	private Long getLongValue(String key, ErrorBean e) {
		List<? extends ErrorKeyValuePair> desc = e.getDescription();
		if (desc != null) {
			for (ErrorKeyValuePair errorKeyValuePair : desc) {
				if (key.equals(errorKeyValuePair.getKey())) {
					BigDecimal v = errorKeyValuePair.getDecimalValue();
					if (v != null) {
						return v.longValue();
					} else {
						return null;
					}
				}
			}
		}
		return null;
	}
	
	protected final RemoteCause extractCause(ErrorBean error) {
		if (error == null) {
			return null;
		}
		return extractCause(error.getCause(), new RemoteCause());
	}

	private RemoteCause extractCause(ErrorBean c, RemoteCause remoteCause) {
		if (c != null) {
			List<? extends StacktraceElementBean> items = c.getStackTraceElements();
			if (items != null) {
				List<StackTraceElement> stackTraceElements = new ArrayList<>();
				remoteCause.setStackTraceElements(stackTraceElements);
				for (StacktraceElementBean stackTraceElementRestBean : items) {
					Integer lineNumber = stackTraceElementRestBean.getLineNumber();
					if (lineNumber == null) {
						lineNumber = -1;
					}
					StackTraceElement ste = new StackTraceElement(
							stackTraceElementRestBean.getClassName(),
							stackTraceElementRestBean.getMethodName(),
							stackTraceElementRestBean.getFileName(),
							lineNumber
					);
					stackTraceElements.add(ste);
				}
			}
			c = c.getCause();
			if (c != null) {
				remoteCause.setParentCause(new RemoteCause());
				extractCause(c, remoteCause.getParentCause());
			}
		}
		return remoteCause;
	}
}
