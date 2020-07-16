package org.bndly.rest.jaxb.context;

/*-
 * #%L
 * REST Global JAXB Context Provider
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

import org.bndly.rest.api.ContextObjectResolver;
import org.bndly.rest.client.api.MessageClassesProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = { GlobalJaxbContextProvider.class, ContextObjectResolver.class }, immediate = true)
public class GlobalJaxbContextProvider implements ContextObjectResolver<JAXBContext> {

	private final List<MessageClassesProvider> messageClassesProviders = new ArrayList<>();
	private final ReadWriteLock messageClassesProvidersLock = new ReentrantReadWriteLock();
	
	private Class[] classesToBeBound = null;

	@Reference(
			bind = "addMessageClassesProvider",
			unbind = "removeMessageClassesProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = MessageClassesProvider.class
	)
	public void addMessageClassesProvider(MessageClassesProvider messageClassesProvider) {
		if (messageClassesProvider != null) {
			messageClassesProvidersLock.writeLock().lock();
			try {
				messageClassesProviders.add(messageClassesProvider);
			} finally {
				messageClassesProvidersLock.writeLock().unlock();
			}
		}
	}

	public void removeMessageClassesProvider(MessageClassesProvider messageClassesProvider) {
		if (messageClassesProvider != null) {
			messageClassesProvidersLock.writeLock().lock();
			try {
				messageClassesProviders.remove(messageClassesProvider);
			} finally {
				messageClassesProvidersLock.writeLock().unlock();
			}
		}
	}

	private void rebuildClassesToBeBound() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		messageClassesProvidersLock.readLock().lock();
		try {
			for (MessageClassesProvider messageClassesProvider : messageClassesProviders) {
				Class<?>[] cls = messageClassesProvider.getAllUseableMessageClasses();
				if (cls != null) {
					for (Class<?> c : cls) {
						classes.add(c);
					}
				}
				Class<?>[] ecls = messageClassesProvider.getAllErrorMessageClasses();
				if (ecls != null) {
					for (Class<?> c : ecls) {
						classes.add(c);
					}
				}
			}
		} finally {
			messageClassesProvidersLock.readLock().unlock();
		}
		classesToBeBound = classes.toArray(new Class[classes.size()]);
	}
	
	@Override
	public JAXBContext getContextObject(Class<?> type) {
		if (!JAXBContext.class.isAssignableFrom(type)) {
			return null;
		}
		try {
			if (classesToBeBound == null) {
				rebuildClassesToBeBound();
			}
			return JAXBContext.newInstance(classesToBeBound);
		} catch (JAXBException ex) {
			throw new IllegalStateException("could not set up jaxb context");
		}
	}
}
