package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.rest.controller.api.ExceptionMapper;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.common.beans.util.ExceptionMessageUtil;
import org.bndly.schema.api.exception.ConstraintViolationException;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.UniqueConstraint;
import java.util.List;
import org.osgi.service.component.annotations.Component;

@Component(service = ExceptionMapper.class)
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

	@Override
	public Response toResponse(ConstraintViolationException e) {
		ErrorRestBean msg = new ErrorRestBean();
		msg.setName("ConstraintViolation");

		ExceptionMessageUtil.createKeyValue(msg, "constraintType", "unique");
		UniqueConstraint constraint = e.getConstraint();
		if (constraint != null) {
			List<Attribute> atts = constraint.getAttributes();
			if (atts != null) {
				for (Attribute attribute : atts) {
					ExceptionMessageUtil.createKeyValue(msg, "attribute", attribute.getName());
				}
			}
			ExceptionMessageUtil.createKeyValue(msg, "holder", constraint.getHolder().getName());
		}
		return Response.status(400).entity(msg);
	}

}
