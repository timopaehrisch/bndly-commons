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

import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceInterceptor;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DelegatingResourceInterceptor implements ResourceInterceptor {

	private final List<ResourceInterceptor> interceptors = new ArrayList<>();

	@Override
	public void beforeResourceResolving(Context context) {
		for (ResourceInterceptor resourceInterceptor : interceptors) {
			resourceInterceptor.beforeResourceResolving(context);
		}
	}
	
	@Override
	public Resource intercept(Resource input) {
		for (ResourceInterceptor resourceInterceptor : interceptors) {
			input = resourceInterceptor.intercept(input);
		}
		return input;
	}

	@Override
	public void doFinally(Context context) {
		for (ResourceInterceptor resourceInterceptor : interceptors) {
			resourceInterceptor.doFinally(context);
		}
	}
	
	public void clear() {
		interceptors.clear();
	}
	
	public void addResourceInterceptor(ResourceInterceptor newInterceptor) {
		if (newInterceptor != null) {
			interceptors.add(0, newInterceptor);
		}
	}

	public void removeResourceInterceptor(ResourceInterceptor interceptor) {
		if (interceptor != null) {
			interceptors.remove(interceptor);
		}
	}
	
}
