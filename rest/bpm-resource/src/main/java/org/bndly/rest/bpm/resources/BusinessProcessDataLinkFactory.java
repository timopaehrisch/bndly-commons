package org.bndly.rest.bpm.resources;

/*-
 * #%L
 * REST BPM Resource
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

import org.bndly.common.lang.IteratorChain;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.LinkFactory;
import org.bndly.rest.bpm.beans.BusinessProcessDefinition;
import java.util.Collections;
import java.util.Iterator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = LinkFactory.class)
public class BusinessProcessDataLinkFactory implements LinkFactory<BusinessProcessDefinition> {

	@Reference
	private ContextProvider contextProvider;
	
	@Override
	public Class<BusinessProcessDefinition> getTargetType() {
		return BusinessProcessDefinition.class;
	}

	@Override
	public Iterator<AtomLinkBean> buildLinks(final BusinessProcessDefinition targetBean, boolean isMessageRoot) {
		if (targetBean == null) {
			return Collections.EMPTY_LIST.iterator();
		}
		final String engineName = targetBean.getEngineName();
		if (engineName == null) {
			return Collections.EMPTY_LIST.iterator();
		}
		
		return new IteratorChain<AtomLinkBean>(
				new Iterator<AtomLinkBean>() {
					private Context currentContext;
					private String diagramResourceName;
					private boolean didReturn;
					
					@Override
					public boolean hasNext() {
						if (didReturn) {
							return false;
						}
						currentContext = contextProvider.getCurrentContext();
						if (currentContext == null) {
							return false;
						}
						diagramResourceName = targetBean.getDiagramResourceName();
						return diagramResourceName != null && !diagramResourceName.isEmpty();
					}

					@Override
					public AtomLinkBean next() {
						if (didReturn) {
							return null;
						}
						try {
							currentContext = contextProvider.getCurrentContext();
							diagramResourceName = targetBean.getDiagramResourceName();
							String href = currentContext.createURIBuilder()
									.pathElement("data")
									.pathElement(engineName)
									.pathElement("view")
									.pathElement(diagramResourceName)
									.build().asString()
							;
							return createAtomLinkBean("diagramData", href, "GET");
						} finally {
							didReturn = true;
						}
					}

					@Override
					public void remove() {
					}
					
				},
				new Iterator<AtomLinkBean>() {
					private Context currentContext;
					private String resourceName;
					private boolean didReturn;
					
					@Override
					public boolean hasNext() {
						if (didReturn) {
							return false;
						}
						currentContext = contextProvider.getCurrentContext();
						if (currentContext == null) {
							return false;
						}
						resourceName = targetBean.getDiagramResourceName();
						return resourceName != null && !resourceName.isEmpty();
					}

					@Override
					public AtomLinkBean next() {
						if (didReturn) {
							return null;
						}
						try {
							currentContext = contextProvider.getCurrentContext();
							resourceName = targetBean.getDiagramResourceName();
							String href = currentContext.createURIBuilder()
									.pathElement("data")
									.pathElement(engineName)
									.pathElement("view")
									.pathElement(resourceName)
									.build().asString()
							;
							return createAtomLinkBean("data", href, "GET");
						} finally {
							didReturn = true;
						}
					}

					@Override
					public void remove() {
					}
					
				}
		);
	}
	

	@Override
	public Iterator<AtomLinkBean> buildLinks() {
		return buildLinks(null, true);
	}
	
	private AtomLinkBean createAtomLinkBean(final String rel, final String href, final String method) {
		return new AtomLinkBean() {
			@Override
			public String getRel() {
				return rel;
			}

			@Override
			public void setRel(String rel) {
				throw new UnsupportedOperationException("This atom link bean is not mutable");
			}

			@Override
			public String getHref() {
				return href;
			}

			@Override
			public void setHref(String href) {
				throw new UnsupportedOperationException("This atom link bean is not mutable");
			}

			@Override
			public String getMethod() {
				return method;
			}

			@Override
			public void setMethod(String method) {
				throw new UnsupportedOperationException("This atom link bean is not mutable");
			}
		};
	}
}
