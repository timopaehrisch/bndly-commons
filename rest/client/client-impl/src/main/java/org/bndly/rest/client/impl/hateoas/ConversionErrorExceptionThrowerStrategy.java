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
import org.bndly.rest.client.exception.ConversionClientException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ConversionErrorExceptionThrowerStrategy implements ExceptionThrower.Strategy {

	@Override
	public void throwException(ErrorBean errorBean, ExceptionThrower.Context context) throws ClientException {
		if ("ConversionError".equals(errorBean.getName())) {
			String clazzName = context.getErrorBeanStringValue("className");
			throw new ConversionClientException("a mapping for the class " + clazzName + " failed in the service. remote message: " + errorBean.getMessage(), context.getCause());
		}
	}
	
}
