package org.bndly.rest.repository.resources.beans.initializer;

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

import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.RenderingListener;
import org.bndly.common.velocity.api.VelocityTemplate;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Initializer.class)
public class TemplateContextAwareInitializer implements Initializer, RenderingListener {

	private final ThreadLocal<TemplateContextAware.TemplateContext> currentTemplateContext = new ThreadLocal<>();
	
	@Reference
	private Renderer renderer;
	
	@Activate
	public void activate() {
		renderer.addRenderingListener(this);
	}

	@Deactivate
	public void deactivate() {
		renderer.removeRenderingListener(this);
	}
	
	@Override
	public boolean canInitialize(Class beanType) {
		return TemplateContextAware.class.isAssignableFrom(beanType);
	}

	@Override
	public void initialize(Object bean) {
		TemplateContextAware.TemplateContext templateContext = currentTemplateContext.get();
		if (templateContext != null) {
			((TemplateContextAware) bean).setTemplateContext(templateContext);
		}
	}
	
	public void setCurrentTemplateContext(TemplateContextAware.TemplateContext templateContext) {
		currentTemplateContext.set(templateContext);
	}
	public void removeCurrentTemplateContext() {
		currentTemplateContext.remove();
		
	}

	@Override
	public void beforeRendering(VelocityTemplate velocityTemplate, Writer writer) {
		TemplateContextAware.TemplateContext templateContext = createTemplateContext(velocityTemplate);
		setCurrentTemplateContext(templateContext);
	}

	@Override
	public void afterRendering(VelocityTemplate velocityTemplate, Writer writer) {
		removeCurrentTemplateContext();
	}
	
	private TemplateContextAware.TemplateContext createTemplateContext(final VelocityTemplate velocityTemplate) {
		final Map<String, Object> vars = new HashMap<>();
		return new TemplateContextAware.TemplateContext() {
			boolean didInitMap;

			@Override
			public Object get(String key) {
				if (!didInitMap) {
					didInitMap = true;
					List<ContextData> ctxData = velocityTemplate.getContextData();
					if (ctxData != null) {
						for (ContextData contextData : ctxData) {
							vars.put(contextData.getKey(), contextData.getValue());
						}
					}
				}
				return vars.get(key);
			}
		};
	}	
}
