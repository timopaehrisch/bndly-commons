package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.repository.Path;
import org.bndly.schema.api.repository.PathBuilder;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SelectorsLookupStrategy implements ContextUriToPathStrategy {
	private final List<String> selectorNames;

	public SelectorsLookupStrategy(List<String> selectorNames) {
		this.selectorNames = selectorNames;
	}
	
	@Override
	public Path buildPath(Context context) {
		ResourceURI.Path p = context.getURI().getPath();
		org.bndly.schema.api.repository.Path repoPath;
		if (p == null || p.getElements() == null || p.getElements().isEmpty()) {
			PathBuilder builder = PathBuilder.newInstance();
			StringBuilder sb = new StringBuilder();
			if (selectorNames != null) {
				for (String selector : selectorNames) {
					sb.append(".").append(selector);
				}
			}
			if (sb.length() > 0) {
				builder.element(sb.toString());
			}
			repoPath = builder.build();
		} else {
			List<String> els = p.getElements();
			PathBuilder builder = PathBuilder.newInstance();
			for (int i = 1; i < els.size(); i++) {
				if (i == (els.size() - 1)) {
					StringBuilder sb = new StringBuilder(els.get(i));
					if (selectorNames != null) {
						for (String selector : selectorNames) {
							sb.append(".").append(selector);
						}
					}
					builder.element(sb.toString());
				} else {
					builder.element(els.get(i));
				}
			}
			repoPath = builder.build();
		}
		return repoPath;
	}
	
}
