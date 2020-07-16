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

import org.bndly.common.velocity.api.VelocityDataProvider;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = ResourceLoader.class)
public class StreamResourceLoaderServiceImpl extends ResourceLoader {

	private final Map<String, VelocityDataProvider> velocityDataProvidersByName = new HashMap<>();
	private final ReadWriteLock velocityDataProvidersLock = new ReentrantReadWriteLock();
	private ExtendedProperties velocityConfig;

	@Reference(
			bind = "addVelocityDataProvider",
			unbind = "removeVelocityDataProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = VelocityDataProvider.class
	)
	public void addVelocityDataProvider(VelocityDataProvider velocityDataProvider) {
		if (velocityDataProvider != null) {
			String name = velocityDataProvider.getName();
			if (name != null && !name.isEmpty()) {
				velocityDataProvidersLock.writeLock().lock();
				try {
					velocityDataProvidersByName.put(name, velocityDataProvider);
				} finally {
					velocityDataProvidersLock.writeLock().unlock();
				}
			}
		}
	}
	
	public void removeVelocityDataProvider(VelocityDataProvider velocityDataProvider) {
		if (velocityDataProvider != null) {
			String name = velocityDataProvider.getName();
			if (name != null) {
				velocityDataProvidersLock.writeLock().lock();
				try {
					VelocityDataProvider vdp = velocityDataProvidersByName.get(name);
					if (vdp == velocityDataProvider) {
						velocityDataProvidersByName.remove(name);
					}
				} finally {
					velocityDataProvidersLock.writeLock().unlock();
				}
			}
		}
	}

	@Override
	public void init(ExtendedProperties configuration) {
		this.velocityConfig = configuration;
	}

	@Override
	public InputStream getResourceStream(String sourceName) throws ResourceNotFoundException {
		velocityDataProvidersLock.readLock().lock();
		try {
			VelocityDataProvider vp = getVelocityDataProviderByFileName(sourceName);
			if (vp == null) {
				for (Map.Entry<String, VelocityDataProvider> entrySet : velocityDataProvidersByName.entrySet()) {
					VelocityDataProvider value = entrySet.getValue();
					InputStream s = value.getStream(sourceName);
					if (s != null) {
						return s;
					}
				}
				throw new ResourceNotFoundException("velocity data provider is null");
			} else {
				sourceName = stripVelocityDataProviderNameFromString(vp, sourceName);
				InputStream stream = vp.getStream(sourceName);
				if (stream == null) {
					throw new ResourceNotFoundException("could not find resource " + sourceName);
				} else {
					return stream;
				}
			}
		} finally {
			velocityDataProvidersLock.readLock().unlock();
		}
	}

	private String stripVelocityDataProviderNameFromString(VelocityDataProvider vp, String sourceName) {
		return sourceName.substring(vp.getName().length() + 1);
	}

	@Override
	public boolean isSourceModified(Resource resource) {
		velocityDataProvidersLock.readLock().lock();
		try {
			VelocityDataProvider vp = getVelocityDataProviderByResource(resource);
			return vp.isModified(stripVelocityDataProviderNameFromString(vp, resource.getName()), resource.getLastModified());
		} finally {
			velocityDataProvidersLock.readLock().unlock();
		}
	}

	@Override
	public long getLastModified(Resource resource) {
		velocityDataProvidersLock.readLock().lock();
		try {
			VelocityDataProvider vp = getVelocityDataProviderByResource(resource);
			return vp.getLastModified(stripVelocityDataProviderNameFromString(vp, resource.getName()));
		} finally {
			velocityDataProvidersLock.readLock().unlock();
		}
	}

	private VelocityDataProvider getVelocityDataProviderByResource(Resource resource) {
		VelocityDataProvider vp = getVelocityDataProviderByFileName(resource.getName());
		if (vp == null) {
			for (Map.Entry<String, VelocityDataProvider> entrySet : velocityDataProvidersByName.entrySet()) {
				VelocityDataProvider value = entrySet.getValue();
				if (value.exists(resource.getName())) {
					vp = value;
					break;
				}
			}
		}
		return vp;
	}

	private VelocityDataProvider getVelocityDataProviderByFileName(String fileName) {
		int i = fileName.indexOf("/");
		if (i < 0) {
			return null;
		}
		String name = fileName.substring(0, i);
		
		return velocityDataProvidersByName.get(name);
	}
	
}
