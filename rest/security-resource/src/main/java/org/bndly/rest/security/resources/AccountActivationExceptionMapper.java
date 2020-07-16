package org.bndly.rest.security.resources;

/*-
 * #%L
 * REST Security Resource
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

import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.api.account.AccountActivationException;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.controller.api.ExceptionMapper;
import org.bndly.rest.controller.api.Response;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ExceptionMapper.class)
public class AccountActivationExceptionMapper implements ExceptionMapper<AccountActivationException> {

	@Override
	public Response toResponse(AccountActivationException throwable) {
		ErrorRestBean errorBean = new ErrorRestBean();
		errorBean.setMessage(throwable.getMessage());
		errorBean.setName("accountActivationFailed");
		return Response.status(StatusWriter.Code.INTERNAL_SERVER_ERROR.getHttpCode()).entity(errorBean);
	}
	
}
