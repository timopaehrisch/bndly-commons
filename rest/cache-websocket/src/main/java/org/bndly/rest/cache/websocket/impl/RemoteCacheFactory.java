package org.bndly.rest.cache.websocket.impl;

/*-
 * #%L
 * REST Cache Websocket
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

import org.bndly.de.rest.websocket.api.Socket;
import org.bndly.de.rest.websocket.api.WebsocketClientConnectionFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = RemoteCacheFactory.class, immediate = true)
public class RemoteCacheFactory {
	private static final Logger LOG = LoggerFactory.getLogger(RemoteCacheFactory.class);
	
	private final List<ConfiguredCache> remoteCaches = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	@Reference
	private WebsocketClientConnectionFactory websocketClientConnectionFactory;
	private final List<Runnable> lazyInits = new ArrayList<>();

	private class ConfiguredCache {
		private final RemoteCacheConfiguration configuration;
		private final RemoteCache remoteCache;

		public ConfiguredCache(RemoteCacheConfiguration configuration, RemoteCache remoteCache) {
			this.configuration = configuration;
			this.remoteCache = remoteCache;
		}

		public RemoteCacheConfiguration getConfiguration() {
			return configuration;
		}

		public RemoteCache getRemoteCache() {
			return remoteCache;
		}
		
	}
	
	@Activate
	public void activate() {
		lock.writeLock().lock();
		try {
			for (Runnable lazyInit : lazyInits) {
				lazyInit.run();
			}
			lazyInits.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Deactivate
	public void deactivate() {
		lock.writeLock().lock();
		try {
			for (ConfiguredCache configuredCache : remoteCaches) {
				RemoteCache remoteCache = configuredCache.getRemoteCache();
				if (remoteCache != null) {
					remoteCache.close();
				}
			}
			remoteCaches.clear();
			lazyInits.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	@Reference(
			bind = "addRemoteCacheConfiguration",
			unbind = "removeRemoteCacheConfiguration",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = RemoteCacheConfiguration.class
	)
	public void addRemoteCacheConfiguration(RemoteCacheConfiguration cacheConfiguration) {
		if (cacheConfiguration != null) {
			lock.writeLock().lock();
			try {
				createRemoteCacheFromConfig(cacheConfiguration);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	public void removeRemoteCacheConfiguration(RemoteCacheConfiguration cacheConfiguration) {
		if (cacheConfiguration != null) {
			lock.writeLock().lock();
			try {
				Iterator<ConfiguredCache> iterator = remoteCaches.iterator();
				while (iterator.hasNext()) {
					ConfiguredCache next = iterator.next();
					if (next.getConfiguration() == cacheConfiguration) {
						RemoteCache remoteCache = next.getRemoteCache();
						if (remoteCache != null) {
							remoteCache.close();
						}
						iterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	private void createRemoteCacheFromConfig(final RemoteCacheConfiguration configuration) {
		String url = configuration.getUrl();
		if (url == null) {
			return;
		}
		if (websocketClientConnectionFactory == null) {
			lazyInits.add(new Runnable() {

				@Override
				public void run() {
					createRemoteCacheFromConfig(configuration);
				}
			});
			return;
		}
		RemoteCacheImpl remoteCacheImpl = new RemoteCacheImpl(this);
		try {
			Socket socket = websocketClientConnectionFactory.connectTo(url, remoteCacheImpl);
			remoteCacheImpl.setSocket(socket);
			ConfiguredCache configuredCache = new ConfiguredCache(configuration, remoteCacheImpl);
			remoteCaches.add(configuredCache);
		} catch (URISyntaxException ex) {
			LOG.error("could not create uri from " + url, ex);
		}
	}

	public void removeRemoteCache(RemoteCacheImpl remoteCacheImpl) {
		lock.writeLock().lock();
		try {
			Iterator<ConfiguredCache> iterator = remoteCaches.iterator();
			while (iterator.hasNext()) {
				ConfiguredCache next = iterator.next();
				if (next.getRemoteCache() == remoteCacheImpl) {
					iterator.remove();
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
}
