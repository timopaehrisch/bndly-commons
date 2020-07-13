package org.bndly.rest.responseprocessor.api;

/*-
 * #%L
 * REST Response Postprocessor
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

import org.bndly.common.graph.BeanGraphIteratorListener;
import org.bndly.common.graph.CompiledBeanIterator;
import org.bndly.common.graph.CompiledBeanIteratorProviderImpl;
import org.bndly.common.graph.DelegatingBeanGraphIteratorListener;
import org.bndly.rest.api.NoOpResourceInterceptor;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceInterceptor;
import org.bndly.rest.jaxb.renderer.JAXBResourceRenderer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = ResourceInterceptor.class)
public class ResponsePostProcessor extends NoOpResourceInterceptor implements ResourceInterceptor, FrameworkListener {

	private final List<BeanGraphIteratorListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	private final DelegatingBeanGraphIteratorListener delegatingBeanGraphIteratorListener = new DelegatingBeanGraphIteratorListener();
	private final AnnotationBasedReferenceDetectorImpl referenceDetector = 
			AnnotationBasedReferenceDetectorImpl.newInstance(
				new Class[] {
					org.bndly.rest.atomlink.api.annotation.Reference.class, XmlRootElement.class
				},
				new Class[] {
					XmlAnyElement.class
				}
			);
	private final AnnotationBasedEntityCollectionDetectorImpl entityCollectionDetector = new AnnotationBasedEntityCollectionDetectorImpl(org.bndly.rest.atomlink.api.annotation.Reference.class, XmlRootElement.class);
	private CompiledBeanIteratorProviderImpl compiledBeanIteratorProvider;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		compiledBeanIteratorProvider = new CompiledBeanIteratorProviderImpl();
		compiledBeanIteratorProvider.setEntityCollectionDetector(entityCollectionDetector);
		compiledBeanIteratorProvider.setReferenceDetector(referenceDetector);
		registerListeners();
		componentContext.getBundleContext().addFrameworkListener(this);
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		componentContext.getBundleContext().removeFrameworkListener(this);
		listenersLock.writeLock().lock();
		try {
			listeners.clear();
		} finally {
			listenersLock.writeLock().unlock();
		}
	}

	@Reference(
			bind = "addListener",
			unbind = "removeListener", 
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = BeanGraphIteratorListener.class
	)
	public void addListener(BeanGraphIteratorListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(listener);
				registerListeners();
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	public void removeListener(BeanGraphIteratorListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(listener);
				registerListeners();
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	public void registerListeners() {
		if (listeners != null) {
			listenersLock.readLock().lock();
			try {
				delegatingBeanGraphIteratorListener.setListeners(Collections.unmodifiableList(listeners));
			} finally {
				listenersLock.readLock().unlock();
			}
		}
	}

	@Override
	public Resource intercept(Resource input) {
		if (JAXBResourceRenderer.JAXBResource.class.isInstance(input)) {
			JAXBResourceRenderer.JAXBResource jaxbResource = (JAXBResourceRenderer.JAXBResource) input;
			postProcess(jaxbResource.getRootObject());
		}
		return input;
	}

	public void postProcess(Object entity) {
		if (entity != null) {
			CompiledBeanIterator compiledBeanIteratorForType = compiledBeanIteratorProvider.getCompiledBeanIteratorForType(entity.getClass());
			if (compiledBeanIteratorForType != null) {
				compiledBeanIteratorForType.traverse(entity, delegatingBeanGraphIteratorListener, null);
			}
		}
	}

	/**
	 * On a framework event flush all created compiled bean iterators, because some classes might have been unloaded.
	 * @param fe the fired framework event
	 */
	@Override
	public void frameworkEvent(FrameworkEvent fe) {
		compiledBeanIteratorProvider.clear();
	}
	
}
