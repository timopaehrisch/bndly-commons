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
import org.bndly.rest.client.exception.ConstraintViolationClientException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ConstraintViolationExceptionThrowerStrategy implements ExceptionThrower.Strategy {

	@Override
	public void throwException(ErrorBean errorBean, ExceptionThrower.Context context) throws ClientException {
		if ("ConstraintViolation".equals(errorBean.getName())) {
			//constraintType:unique
				//attribute -> has multiple
				//holder

			// OR
			//constraintType:integrity
			String clazzName = context.getErrorBeanStringValue("holder");
			String fieldName = context.getErrorBeanStringValue("fieldName");
			String constraintType = context.getErrorBeanStringValue("constraintType");
			if ("integrity".equals(constraintType)) {
				// ignore
			} else if ("unique".equals(constraintType)) {
				String[] attributes = context.getErrorBeanStringValues("attribute");
				StringBuffer sb = null;
				for (String attribute : attributes) {
					if (sb == null) {
						sb = new StringBuffer("[");
					} else {
						sb.append(',');
					}
					sb.append(attribute);
				}
				if (sb != null) {
					sb.append("]");
				}
				fieldName = sb == null ? null : sb.toString();
			} else {
				constraintType = "unknown";
			}
			throw new ConstraintViolationClientException(
				clazzName, fieldName, constraintType, 
					"request caused a " + constraintType + " constraint violation "
					+ "in bean " + clazzName + " on field(s) " + fieldName
					+ " remote message: " + context.getErrorBean().getMessage()
			);
		}
	}
	
}
