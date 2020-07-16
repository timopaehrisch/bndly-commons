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
public interface ContextUriToPathStrategy {
	public ContextUriToPathStrategy URI_PATH_ONLY = new ContextUriToPathStrategy() {
		@Override
		public org.bndly.schema.api.repository.Path buildPath(Context context) {
			ResourceURI.Path p = context.getURI().getPath();
			org.bndly.schema.api.repository.Path repoPath;
			if (p == null) {
				repoPath = PathBuilder.newInstance().build();
			} else {
				List<String> els = p.getElements();
				if (els == null || els.size() < 1) {
					repoPath = PathBuilder.newInstance().build();
				} else {
					PathBuilder builder = PathBuilder.newInstance();
					for (int i = 1; i < els.size(); i++) {
						builder.element(els.get(i));
					}
					repoPath = builder.build();
				}
			}
			return repoPath;
		}
	};
	
	public ContextUriToPathStrategy URI_WITH_SELECTORS_AND_EXTENSION = new ContextUriToPathStrategy() {
		@Override
		public org.bndly.schema.api.repository.Path buildPath(Context context) {
			ResourceURI.Path p = context.getURI().getPath();
			org.bndly.schema.api.repository.Path repoPath;
			if (p == null || p.getElements() == null || p.getElements().isEmpty()) {
				PathBuilder builder = PathBuilder.newInstance();
				StringBuilder sb = new StringBuilder();
				List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
				if (selectors != null) {
					for (ResourceURI.Selector selector : selectors) {
						sb.append(".").append(selector.getName());
					}
				}
				ResourceURI.Extension extension = context.getURI().getExtension();
				if (extension != null) {
					sb.append(".").append(extension.getName());
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
						List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
						if (selectors != null) {
							for (ResourceURI.Selector selector : selectors) {
								sb.append(".").append(selector.getName());
							}
						}
						ResourceURI.Extension extension = context.getURI().getExtension();
						if (extension != null) {
							sb.append(".").append(extension.getName());
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
	};
	
	public ContextUriToPathStrategy URI_WITH_SELECTORS = new ContextUriToPathStrategy() {
		@Override
		public org.bndly.schema.api.repository.Path buildPath(Context context) {
			ResourceURI.Path p = context.getURI().getPath();
			org.bndly.schema.api.repository.Path repoPath;
			if (p == null || p.getElements() == null || p.getElements().isEmpty()) {
				PathBuilder builder = PathBuilder.newInstance();
				StringBuilder sb = new StringBuilder();
				List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
				if (selectors != null) {
					for (ResourceURI.Selector selector : selectors) {
						sb.append(".").append(selector.getName());
					}
				}
				ResourceURI.Extension extension = context.getURI().getExtension();
				if (extension != null) {
					sb.append(".").append(extension.getName());
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
						List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
						if (selectors != null) {
							for (ResourceURI.Selector selector : selectors) {
								sb.append(".").append(selector.getName());
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
	};

	Path buildPath(Context context);
}
