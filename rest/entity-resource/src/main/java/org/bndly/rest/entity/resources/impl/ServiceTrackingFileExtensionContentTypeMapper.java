package org.bndly.rest.entity.resources.impl;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.data.api.FileExtensionContentTypeMapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link FileExtensionContentTypeMapper} that delegates all calls to one of the
 * {@link FileExtensionContentTypeMapper} services registered with the OSGi service registry.
 * <p>
 * This class extends {@link ServiceTracker}, i.e. make sure to properly invoke {@link #open()} and {@link #close()}
 * when using it!
 */
public class ServiceTrackingFileExtensionContentTypeMapper
		extends ServiceTracker<FileExtensionContentTypeMapper, FileExtensionContentTypeMapper>
		implements FileExtensionContentTypeMapper {

	/**
	 * Keeps track of known {@link FileExtensionContentTypeMapper} services
	 */
	private final List<FileExtensionContentTypeMapper> knownServiceInstances = new ArrayList<>();

	/**
	 * Guards concurrent access to the {@link #knownServiceInstances} collection
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public ServiceTrackingFileExtensionContentTypeMapper(BundleContext bundleContext) {
		super(bundleContext, FileExtensionContentTypeMapper.class, null);
	}

	@Override
	public FileExtensionContentTypeMapper addingService(ServiceReference<FileExtensionContentTypeMapper> serviceReference) {
		lock.writeLock().lock();
		try {
			final FileExtensionContentTypeMapper serviceInstance = super.addingService(serviceReference);
			knownServiceInstances.add(serviceInstance);
			return serviceInstance;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removedService(ServiceReference<FileExtensionContentTypeMapper> reference, FileExtensionContentTypeMapper serviceInstance) {
		lock.writeLock().lock();
		try {
			final Iterator<FileExtensionContentTypeMapper> serviceIt = knownServiceInstances.iterator();
			while(serviceIt.hasNext()) {
				if(serviceIt.next() == serviceInstance) {
					serviceIt.remove();
				}
			}
			super.removedService(reference, serviceInstance);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Tries to determine a content type for the supplied file extension using the known {@link FileExtensionContentTypeMapper} services
	 *
	 * @param extension File extension to be mapped
	 * @return Content type determined by the first {@link FileExtensionContentTypeMapper} returning a non-null result
	 * or {@code null} if none of the known mappers returns a non-null result.
	 */
	@Override
	public String mapExtensionToContentType(String extension) {
		lock.readLock().lock();
		try {
			for (FileExtensionContentTypeMapper mapper : knownServiceInstances) {
				String contentType = mapper.mapExtensionToContentType(extension);
				if (contentType != null) {
					return contentType;
				}
			}
		} finally {
			lock.readLock().unlock();
		}

		return null;
	}

	/**
	 * Tries to determine a file extension for the supplied content type using the known {@link FileExtensionContentTypeMapper} services
	 *
	 * @param contentType Content type to be mapped
	 * @return File extension determined by the first {@link FileExtensionContentTypeMapper} returning a non-null result
	 * or {@code null} if none of the known mappers returns a non-null result.
	 */
	@Override
	public String mapContentTypeToExtension(String contentType) {
		lock.readLock().lock();
		try {
			for (FileExtensionContentTypeMapper mapper : knownServiceInstances) {
				String extension = mapper.mapContentTypeToExtension(contentType);
				if (extension != null) {
					return extension;
				}
			}
		} finally {
			lock.readLock().unlock();
		}

		return null;
	}
}
