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
import org.bndly.rest.api.DelegatingResourceProvider;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceBuildingException;
import org.bndly.rest.api.ResourceProvider;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DelegatingResourceProviderImpl implements DelegatingResourceProvider {

	private final List<ResourceProvider> providers = new ArrayList<>();

	@Override
	public void addResourceProvider(ResourceProvider resourceProvider) {
		if (resourceProvider != null) {
			providers.add(0, resourceProvider);
		}
	}

	public void clear() {
		providers.clear();
	}

	@Override
	public void removeResourceProvider(ResourceProvider resourceProvider) {
		if (resourceProvider != null) {
			int index = -1;
			for (int i = 0; i < providers.size(); i++) {
				ResourceProvider resourceProvider1 = providers.get(i);
				if (resourceProvider1 == resourceProvider) {
					index = i;
					break;
				}
			}
			if (index > -1) {
				providers.remove(index);
			}
		}
	}

	@Override
	public boolean supports(Context context) {
		for (ResourceProvider resourceProvider : providers) {
			if (resourceProvider.supports(context)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Resource build(Context context, ResourceProvider provider) throws ResourceBuildingException {
		for (ResourceProvider resourceProvider : providers) {
			if (resourceProvider.supports(context)) {
				return resourceProvider.build(context, provider);
			}
		}
		throw new NoSupportingResourceProviderException();
	}

}
