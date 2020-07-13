package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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

import org.bndly.rest.api.SecurityContext;
import org.bndly.rest.api.SecurityHandler;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SecurityContextImpl implements SecurityContext {

	private final ContextImpl context;

	public SecurityContextImpl(ContextImpl context) {
		this.context = context;
	}

	@Override
	public boolean isServableContext() {
		SecurityHandler handler = context.getSecurityHandler();
		if (handler == null) {
			return false;
		}
		return handler.isServableContext(context);
	}

	@Override
	public void invalidateAuthenticationData() {
		SecurityHandler handler = context.getSecurityHandler();
		if (handler == null) {
			return;
		}
		handler.invalidateAuthenticationData(context);
	}

}
