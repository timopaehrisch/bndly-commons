package org.bndly.common.velocity.impl;

/*-
 * #%L
 * Velocity
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

import org.bndly.common.reflection.FieldBeanPropertyAccessor;
import org.apache.velocity.ResourceLoaderAdapter;
import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.DirectiveProvider;
import org.bndly.common.velocity.api.PartialRenderer;
import org.bndly.common.velocity.api.TranslationResolver;
import org.bndly.common.velocity.api.VelocityTemplate;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.RenderingListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = Renderer.class)
@Designate(ocd = RendererImpl.Configuration.class)
public class RendererImpl implements Renderer {

	@ObjectClassDefinition(
			name = "Velocity Template Renderer",
			description = "This renderer will render velocity templates to the provided streams/writers"
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Output Encoding",
				description = "The encoding of the output stream when rendering a template"
		)
		String outputEncoding() default "UTF-8";
		
		@AttributeDefinition(
				name = "Input Encoding",
				description = "The encoding of the input streams like the template files"
		)
		String inputEncoding() default "UTF-8";
		
		@AttributeDefinition(
				name = "Enable Resource Caching", 
				description = "If set to true, the resources of the templating engine will be cached for a fixed interval."
		)
		boolean enableResourceCaching() default false;
		
		@AttributeDefinition(
				name = "Resource Modification Check Interval", 
				description = "The interval of resource modification checks in seconds. values lower or equal 0 mean, that there will be no modification checking."
		)
		int modificationCheckInterval() default 0;
	}
	
	private String outputEncoding;
	private String inputEncoding;
	private VelocityEngine velocityEngine;
	private TranslationResolver translationResolver;
	private final List<ResourceLoader> resourceLoaders = new ArrayList<>();
	private final ReadWriteLock resourceLoadersLock = new ReentrantReadWriteLock();
	private final List<DirectiveProvider> directiveProviders = new ArrayList<>();
	private final ReadWriteLock directiveProvidersLock = new ReentrantReadWriteLock();
	
	private RuntimeInstance runtimeInstance;
	private final List<RenderingListener> renderingListeners = new ArrayList<>();
	private final ReadWriteLock renderingListenerLock = new ReentrantReadWriteLock();

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			bind = "addResourceLoader", 
			unbind = "removeResourceLoader",
			policy = ReferencePolicy.DYNAMIC,
			service = ResourceLoader.class
	)
	public void addResourceLoader(ResourceLoader resourceLoader) {
		if (resourceLoader != null) {
			resourceLoadersLock.writeLock().lock();
			try {
				resourceLoaders.add(resourceLoader);
			} finally {
				resourceLoadersLock.writeLock().unlock();
			}
		}
	}

	public void removeResourceLoader(ResourceLoader resourceLoader) {
		if (resourceLoader != null) {
			resourceLoadersLock.writeLock().lock();
			try {
				resourceLoaders.remove(resourceLoader);
			} finally {
				resourceLoadersLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			bind = "addDirectiveProvider", 
			unbind = "removeDirectiveProvider",
			policy = ReferencePolicy.DYNAMIC,
			service = DirectiveProvider.class
	)
	public void addDirectiveProvider(DirectiveProvider directiveProvider) {
		if (directiveProvider != null) {
			directiveProvidersLock.writeLock().lock();
			try {
				directiveProviders.add(directiveProvider);
				if (runtimeInstance != null) {
					directiveProvider.registerDirectives(runtimeInstance);
				}
			} finally {
				directiveProvidersLock.writeLock().unlock();
			}
		}
	}

	public void removeDirectiveProvider(DirectiveProvider directiveProvider) {
		if (directiveProvider != null) {
			directiveProvidersLock.writeLock().lock();
			try {
				Iterator<DirectiveProvider> iterator = directiveProviders.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == directiveProvider) {
						iterator.remove();
					}
				}
				if (runtimeInstance != null) {
					directiveProvider.unregisterDirectives(runtimeInstance);
				}
			} finally {
				directiveProvidersLock.writeLock().unlock();
			}
		}
	}
	
	@Override
	public void addRenderingListener(RenderingListener listener) {
		if (listener == null) {
			return;
		}
		renderingListenerLock.writeLock().lock();
		try {
			renderingListeners.add(listener);
		} finally {
			renderingListenerLock.writeLock().unlock();
		}
	}

	@Override
	public void removeRenderingListener(RenderingListener listener) {
		if (listener == null) {
			return;
		}
		renderingListenerLock.writeLock().lock();
		try {
			Iterator<RenderingListener> iterator = renderingListeners.iterator();
			while (iterator.hasNext()) {
				if (iterator.next() == listener) {
					iterator.remove();
				}
			}
		} finally {
			renderingListenerLock.writeLock().unlock();
		}
	}
	
	@Activate
	public final void activate(Configuration configuration) {
		outputEncoding = configuration.outputEncoding();
		inputEncoding = configuration.inputEncoding();
		ResourceLoaderAdapter.setDelegate(new ResourceLoader() {

			@Override
			public void init(ExtendedProperties configuration) {
				resourceLoadersLock.readLock().lock();
				try {
					for (ResourceLoader resourceLoader : resourceLoaders) {
						resourceLoader.init(configuration);
					}
				} finally {
					resourceLoadersLock.readLock().unlock();
				}
			}

			@Override
			public InputStream getResourceStream(String source) throws ResourceNotFoundException {
				resourceLoadersLock.readLock().lock();
				try {
					for (ResourceLoader resourceLoader : resourceLoaders) {
						try {
							return resourceLoader.getResourceStream(source);
						} catch (ResourceNotFoundException e) {
							// ignore the exception here. throw own exception later.
						}
					}
					throw new ResourceNotFoundException("could not find resource: " + source);
				} finally {
					resourceLoadersLock.readLock().unlock();
				}
			}

			@Override
			public boolean isSourceModified(Resource resource) {
				resourceLoadersLock.readLock().lock();
				try {
					for (ResourceLoader resourceLoader : resourceLoaders) {
						return resourceLoader.isSourceModified(resource);
					}
					return false;
				} finally {
					resourceLoadersLock.readLock().unlock();
				}
			}

			@Override
			public long getLastModified(Resource resource) {
				resourceLoadersLock.readLock().lock();
				try {
					for (ResourceLoader resourceLoader : resourceLoaders) {
						return resourceLoader.getLastModified(resource);
					}
					return 0;
				} finally {
					resourceLoadersLock.readLock().unlock();
				}
			}
		});
		boolean enableCaching = configuration.enableResourceCaching();
		int modificationCheckInterval = configuration.modificationCheckInterval();
		VelocityEngine vengine = new VelocityEngine();
		vengine.setProperty(RuntimeConstants.RESOURCE_LOADER, "adapter,classpath");
		vengine.setProperty("adapter.resource.loader.class", ResourceLoaderAdapter.class.getName());
		vengine.setProperty("adapter.resource.loader.cache", enableCaching);
		vengine.setProperty("adapter.resource.loader.modificationCheckInterval", Integer.toString(modificationCheckInterval)); // velocity expects this property as a string
		vengine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		vengine.setProperty(RuntimeConstants.INPUT_ENCODING, inputEncoding);
		vengine.setProperty(RuntimeConstants.OUTPUT_ENCODING, outputEncoding);
		vengine.init();
		runtimeInstance = (RuntimeInstance) new FieldBeanPropertyAccessor().get("ri", vengine);
		if (runtimeInstance != null) {
			directiveProvidersLock.readLock().lock();
			try {
				for (DirectiveProvider directiveProvider : directiveProviders) {
					directiveProvider.registerDirectives(runtimeInstance);
				}
			} finally {
				directiveProvidersLock.readLock().unlock();
			}
		}
		velocityEngine = vengine;
	}
	
	@Deactivate
	public final void deactivate() {
		velocityEngine = null;
		if (runtimeInstance != null) {
			directiveProvidersLock.writeLock().lock();
			try {
				for (DirectiveProvider directiveProvider : directiveProviders) {
					directiveProvider.unregisterDirectives(runtimeInstance);
				}
				directiveProviders.clear();
			} finally {
				directiveProvidersLock.writeLock().unlock();
			}
		}
		runtimeInstance = null;
	}

	@Override
	public boolean isTemplateAvailable(String templateName) {
		return velocityEngine.resourceExists(templateName);
	}

	@Override
	public void render(final VelocityTemplate template, Writer writer) {
		renderingListenerLock.readLock().lock();
		try {
			for (RenderingListener renderingListener : renderingListeners) {
				renderingListener.beforeRendering(template, writer);
			}
			Template t = velocityEngine.getTemplate(template.getTemplateName());
			VelocityContext ctx = new VelocityContext();
			List<ContextData> ctxData = template.getContextData();
			if (ctxData != null) {
				for (ContextData contextData : ctxData) {
					ctx.put(contextData.getKey(), contextData.getValue());
				}
			}

			ctx.put("partialRenderer", new PartialRenderer() {
				@Override
				public String include(Object entity, String templateName) {
					return renderPartialToString(entity, templateName, template);
				}

				@Override
				public void include(Object entity, String templateName, Writer writer) throws IOException {
					renderPartialToWriter(entity, templateName, template, writer);
				}

			});
			ctx.put("translator", new TranslatorImpl(template, translationResolver));
			ctx.put("priceFormatter", new PriceFormatterImpl(template));
			ctx.put("dateFormatter", new DateFormatterImpl(template));

			ctx.put("model", template.getEntity());
			ctx.put("template", template);
			ctx.put("null", NullTool.INSTANCE);
			ctx.put("ctx", ctx);
			t.merge(ctx, writer);
			for (RenderingListener renderingListener : renderingListeners) {
				renderingListener.afterRendering(template, writer);
			}
		} finally {
			renderingListenerLock.readLock().unlock();
		}
	}

	public void render(final VelocityTemplate template, OutputStream os) {
		OutputStreamWriter writer = new OutputStreamWriter(os);
		render(template, writer);
		try {
			writer.flush();
		} catch (IOException ex) {
			throw new IllegalStateException("could not flush:" + ex.getMessage(), ex);
		}
	}

	private void renderPartialToWriter(Object entity, String templateName, VelocityTemplate parentTemplate, Writer writer) throws IOException {
		VelocityTemplate template = new VelocityTemplate();
		template.setEntity(entity);
		template.setTemplateName(templateName);
		template.setLocale(parentTemplate.getLocale());
		template.setContextData(parentTemplate.getContextData());
		template.setDateFormatString(parentTemplate.getDateFormatString());
		render(template, writer);
	}
	
	private String renderPartialToString(Object entity, String templateName, VelocityTemplate parentTemplate) {
		StringWriter sw = new StringWriter();
		try {
			renderPartialToWriter(entity, templateName, parentTemplate, sw);
			sw.flush();
		} catch (IOException ex) {
			throw new IllegalStateException("could not render partial", ex);
		}
		return sw.toString();
	}

	public void unsetTranslationResolver(TranslationResolver translationResolver) {
		if (this.translationResolver == translationResolver) {
			this.translationResolver = null;
		}
	}

	@Reference(
			bind = "setTranslationResolver",
			unbind = "unsetTranslationResolver",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = TranslationResolver.class
	)
	public void setTranslationResolver(TranslationResolver translationResolver) {
		this.translationResolver = translationResolver;
	}

}
