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
import org.bndly.rest.client.api.ExceptionThrower;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.client.exception.UnallowedOperationClientException;
import org.bndly.rest.client.exception.UnmanagedClientException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpStatus;

public class ExceptionThrowerImpl implements ExceptionThrower {
	
	private final List<ExceptionThrower.Strategy> strategies;
	
	public static final List<ExceptionThrower.Strategy> DEFAULT_STRATEGIES;
	
	static {
		List<ExceptionThrower.Strategy> tmp = new ArrayList<>();
		tmp.add(new ConstraintViolationExceptionThrowerStrategy());
		tmp.add(new ConversionErrorExceptionThrowerStrategy());
		tmp.add(new FatalErrorExceptionThrowerStrategy());
		tmp.add(new MissingParameterErrorExceptionThrowerStrategy());
		tmp.add(new NoRequestBodyErrorExceptionThrowerStrategy());
		tmp.add(new PageOutOfBoundsErrorExceptionThrowerStrategy());
		tmp.add(new UnallowedOperationErrorExceptionThrowerStrategy());
		tmp.add(new UnknownResourceErrorExceptionThrowerStrategy());
		tmp.add(new BadArgumentErrorExceptionThrowerStrategy());
		tmp.add(new ProcessErrorExceptionThrowerStrategy());
		tmp.add(new StaleObjectModificationErrorExceptionThrowerStrategy());
		DEFAULT_STRATEGIES = Collections.unmodifiableList(tmp);
	}

	public ExceptionThrowerImpl(List<Strategy> strategies) {
		this.strategies = strategies;
	}
	
	public ExceptionThrowerImpl() {
		this.strategies = new ArrayList<>(DEFAULT_STRATEGIES);
	}

	@Override
	public void throwException(Object error, final int statusCode, final String httpMethod, final String url) throws ClientException {
		Context context = new ContextImpl((ErrorBean.class.isInstance(error) ? (ErrorBean)error : null), statusCode, httpMethod, url);
		if (context.getErrorBean() != null) {
			for (Strategy strategy : strategies) {
				strategy.throwException(context.getErrorBean(), context);
			}
		}

		if (statusCode == HttpStatus.SC_FORBIDDEN || statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new UnallowedOperationClientException("request was not allowed:  " + httpMethod + " " + url, context.getCause());
		}

		String message = "status " + statusCode + " on " + httpMethod + " for " + url;
		throw new UnmanagedClientException(statusCode, url, httpMethod, message);
	}

}
