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
import org.bndly.rest.common.beans.error.ErrorKeyValuePairRestBean;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.osgi.service.component.annotations.Component;

@Component(service = ExceptionMapper.class)
public class PaginationExceptionMapper implements ExceptionMapper<PaginationException> {

	@Override
	public Response toResponse(PaginationException e) {
		ErrorRestBean msg = new ErrorRestBean();
		msg.setName("PageOutOfBoundsError");
		List<ErrorKeyValuePairRestBean> desc = new ArrayList<>();
		msg.setDescription(desc);
		createKeyValue(desc, "start", e.getStart());
		createKeyValue(desc, "totalCount", e.getTotalRecords());
		createKeyValue(desc, "size", e.getSize());
		return Response.status(404).entity(msg);
	}

	private void createKeyValue(List<ErrorKeyValuePairRestBean> desc, String key, Long value) {
		ErrorKeyValuePairRestBean kv = new ErrorKeyValuePairRestBean();
		kv.setKey(key);
		kv.setDecimalValue(new BigDecimal(value));
		desc.add(kv);
	}
}
