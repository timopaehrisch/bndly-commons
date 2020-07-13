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
import org.bndly.rest.api.ResourceRenderer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DelegatingResourceRenderer implements ResourceRenderer {

	private final List<ResourceRenderer> renderers = new ArrayList<>();

	public void clear() {
		renderers.clear();
	}

	@Override
	public boolean supports(Resource resource, Context context) {
		for (ResourceRenderer resourceRenderer : renderers) {
			if (resourceRenderer.supports(resource, context)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		for (ResourceRenderer resourceRenderer : renderers) {
			if (resourceRenderer.supports(resource, context)) {
				resourceRenderer.render(resource, context);
				return;
			}
		}
	}

	public void addResourceRenderer(ResourceRenderer resourceRenderer) {
		if (resourceRenderer != null) {
			renderers.add(0, resourceRenderer);
		}
	}

	public void removeResourceRenderer(ResourceRenderer resourceRenderer) {
		for (int i = 0; i < renderers.size(); i++) {
			ResourceRenderer resourceRenderer1 = renderers.get(i);
			if (resourceRenderer1 == resourceRenderer) {
				renderers.remove(i);
			}
		}
	}
}
