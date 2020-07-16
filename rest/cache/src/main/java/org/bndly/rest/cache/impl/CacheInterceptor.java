package org.bndly.rest.cache.impl;

/*-
 * #%L
 * REST Cache
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

import org.bndly.common.crypto.api.HashService;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceInterceptor;
import org.bndly.rest.cache.api.CacheEventListener;
import org.bndly.rest.cache.api.CacheLink;
import org.bndly.rest.cache.api.CacheLinkingService;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.cache.api.CacheTransactionListener;
import org.bndly.rest.cache.api.ContextCacheTransactionProvider;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {}, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = CacheInterceptor.Configuration.class)
public class CacheInterceptor implements ResourceInterceptor, CacheLinkingService, CacheTransactionFactory, ContextCacheTransactionProvider {

	private ServiceRegistration reg;

	@ObjectClassDefinition(
		name = "Cache Interceptor"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Cache root",
				description = "The folder in which the cached responses should be stored"
		)
		String cacheRoot() default "restcache";

		@AttributeDefinition(
				name = "Skip caching",
				description = "If this property is true, then no caching operations will be performed"
		)
		boolean skip() default false;
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(CacheInterceptor.class);
	private final CacheEntryIO cacheEntryIO = new CacheEntryIO();
	private String cacheRoot = "restcache";
	@Reference
	private HashService hashService;
	private final List<CacheEventListener> cacheEventListeners = new ArrayList<>();
	private final ReadWriteLock cacheEventListenersLock = new ReentrantReadWriteLock();
	private final List<CacheTransactionListener> cacheTransactionListeners = new ArrayList<>();
	private final ReadWriteLock cacheTransactionListenersLock = new ReentrantReadWriteLock();
	private final ThreadLocal<CacheHandlerImpl> currentCacheHandler = new ThreadLocal<>();
	private boolean skip;
	
	@Reference(
			bind = "addCacheEventListener",
			unbind = "removeCacheEventListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = CacheEventListener.class
	)
	public void addCacheEventListener(CacheEventListener cacheEventListener) {
		if (cacheEventListener != null) {
			cacheEventListenersLock.writeLock().lock();
			try {
				cacheEventListeners.add(cacheEventListener);
			} finally {
				cacheEventListenersLock.writeLock().unlock();
			}
		}
	}

	public void removeCacheEventListener(CacheEventListener cacheEventListener) {
		if (cacheEventListener != null) {
			cacheEventListenersLock.writeLock().lock();
			try {
				Iterator<CacheEventListener> iterator = cacheEventListeners.iterator();
				while (iterator.hasNext()) {
					CacheEventListener next = iterator.next();
					if (next == cacheEventListener) {
						iterator.remove();
					}
				}
			} finally {
				cacheEventListenersLock.writeLock().unlock();
			}
		}
	}

	private static class NoOp implements CacheLinkingService, CacheTransactionFactory, ContextCacheTransactionProvider {

		private final CacheTransaction noOpTx = new CacheTransaction() {
			@Override
			public void flush() {

			}

			@Override
			public void flush(String path) {

			}

			@Override
			public void flushRecursive(String path) {

			}

			@Override
			public void replayEvents(CacheEventListener cacheEventListener) {

			}

			@Override
			public void close() {

			}
		};

		@Override
		public void link(String entryPath, String linkTargetPath) throws IOException {

		}

		@Override
		public void iterateLinksOf(String entryPath, Consumer consumer) throws IOException {

		}

		@Override
		public CacheTransaction createCacheTransaction() {
			return noOpTx;
		}

		@Override
		public CacheTransaction createCacheTransactionWithSuppressedEventing() {
			return noOpTx;
		}

		@Override
		public void addCacheTransactionListener(CacheTransactionListener cacheTransactionListener) {

		}

		@Override
		public void removeCacheTransactionListener(CacheTransactionListener cacheTransactionListener) {

		}

		@Override
		public CacheTransaction getCacheTransaction() {
			return noOpTx;
		}

		@Override
		public CacheTransaction getCacheTransactionWithSuppressedEventing() {
			return noOpTx;
		}
	}

	@Activate
	public void activate(Configuration configuration, BundleContext bundleContext) {
		cacheRoot = configuration.cacheRoot();
		skip = configuration.skip();
		if (!skip) {
			LOG.info("cache interceptor will cache resources");
			reg = ServiceRegistrationBuilder.newInstance(this)
					.serviceInterface(ResourceInterceptor.class)
					.serviceInterface(CacheLinkingService.class)
					.serviceInterface(CacheTransactionFactory.class)
					.serviceInterface(ContextCacheTransactionProvider.class)
					.register(bundleContext);
		} else {
			LOG.info("cache interceptor will skip resource caching");
			reg = ServiceRegistrationBuilder.newInstance(new NoOp())
					.serviceInterface(CacheLinkingService.class)
					.serviceInterface(CacheTransactionFactory.class)
					.serviceInterface(ContextCacheTransactionProvider.class)
					.register(bundleContext);
		}
	}

	@Deactivate
	public void deactivate() {
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}

	@Override
	public void addCacheTransactionListener(CacheTransactionListener cacheTransactionListener) {
		if (cacheTransactionListener != null) {
			cacheTransactionListenersLock.writeLock().lock();
			try {
				cacheTransactionListeners.add(cacheTransactionListener);
			} finally {
				cacheTransactionListenersLock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeCacheTransactionListener(CacheTransactionListener cacheTransactionListener) {
		if (cacheTransactionListener != null) {
			cacheTransactionListenersLock.writeLock().lock();
			try {
				Iterator<CacheTransactionListener> iterator = cacheTransactionListeners.iterator();
				while (iterator.hasNext()) {
					CacheTransactionListener next = iterator.next();
					if (next == cacheTransactionListener) {
						iterator.remove();
					}
				}
			} finally {
				cacheTransactionListenersLock.writeLock().unlock();
			}
		}
	}

	@Override
	public void beforeResourceResolving(Context context) {
		CacheHandlerImpl handler = new CacheHandlerImpl(this, hashService);
		currentCacheHandler.set(handler);
		context.setCacheHandler(handler);
	}

	@Override
	public void doFinally(Context context) {
		currentCacheHandler.remove();
	}

	public boolean isCachingSkipped() {
		return skip;
	}

	@Override
	public CacheTransaction getCacheTransaction() {
		CacheHandlerImpl handler = currentCacheHandler.get();
		if (handler == null) {
			return null;
		}
		return handler.getCacheTransaction();
	}

	@Override
	public CacheTransaction getCacheTransactionWithSuppressedEventing() {
		CacheHandlerImpl handler = currentCacheHandler.get();
		if (handler == null) {
			return null;
		}
		return handler.getCacheTransactionWithSuppressedEventing();
	}
	
	public final String getCacheRoot() {
		return cacheRoot;
	}
	
	public final void setCacheRoot(String cacheRoot) {
		this.cacheRoot = cacheRoot;
	}
	
	@Override
	public Resource intercept(Resource input) {
		return input;
	}
	
	public final CacheEntryIO getCacheEntryIO() {
		return cacheEntryIO;
	}

	@Override
	public CacheTransaction createCacheTransaction() {
		return createCacheTransaction(false);
	}

	@Override
	public CacheTransaction createCacheTransactionWithSuppressedEventing() {
		return createCacheTransaction(true);
	}

	private CacheTransaction createCacheTransaction(final boolean suppressEventing) {
		if (skip) {
			return NoOpCacheTransaction.INSTANCE;
		}
		return new CacheTransaction() {
			
			boolean didCommit = false;
			boolean flushEverything = false;
			Set<String> flushedPaths = new HashSet<>();
			Set<String> recursiveFlushedPaths = new HashSet<>();

			@Override
			public void flush() {
				if (didCommit) {
					LOG.warn("flushing cache with an already committed cache transaction");
					return;
				}
				flushEverything = true;
			}

			@Override
			public void flush(String path) {
				if (didCommit) {
					LOG.warn("flushing cache with an already committed cache transaction");
					return;
				}
				flushedPaths.add(path);
			}

			@Override
			public void flushRecursive(String path) {
				if (didCommit) {
					LOG.warn("flushing cache with an already committed cache transaction");
					return;
				}
				recursiveFlushedPaths.add(path);
			}

			@Override
			public void replayEvents(CacheEventListener cacheEventListener) {
				if (cacheEventListener == null) {
					return;
				}
				if (flushEverything) {
					cacheEventListener.onFlush();
					return;
				}
				for (String flushedPath : flushedPaths) {
					cacheEventListener.onFlush(flushedPath, false);
				}
				for (String flushedPath : recursiveFlushedPaths) {
					cacheEventListener.onFlush(flushedPath, true);
				}
			}

			@Override
			public void close() {
				if (didCommit) {
					LOG.warn("committing an already committed cache transaction");
					return;
				}
				LOG.debug("committing cache transaction");
				try {
					if (flushEverything) {
						internalFlush(suppressEventing);
						return;
					}
					try {
						for (String flushedPath : flushedPaths) {
							internalFlush(flushedPath, false, suppressEventing);
						}
						for (String recursiveFlushedPath : recursiveFlushedPaths) {
							internalFlush(recursiveFlushedPath, true, suppressEventing);
						}
					} finally {
						flushedPaths.clear();
						recursiveFlushedPaths.clear();
					}
					LOG.debug("successfully commited cache transaction");
				} finally {
					didCommit = true;
					cacheTransactionListenersLock.readLock().lock();
					List<CacheTransactionListener> listenersToUse;
					try {
						listenersToUse = !suppressEventing ? new ArrayList<>(cacheTransactionListeners) : Collections.EMPTY_LIST;
					} finally {
						cacheTransactionListenersLock.readLock().unlock();
					}
					for (CacheTransactionListener listener : listenersToUse) {
						try {
							listener.onCommit(this);
						} catch (Exception e) {
							LOG.error("cache transaction listener threw an exception: " + e.getMessage(), e);
						}
					}
				}
			}
		};
	}
	
	private void internalFlush(boolean suppressEventing) {
		FlushStrategy strategy;
		if (!suppressEventing) {
			cacheEventListenersLock.readLock().lock();
			try {
				strategy = new FlushEverything(cacheRoot, new ArrayList<>(cacheEventListeners)); // defensive copy
			} finally {
				cacheEventListenersLock.readLock().unlock();
			}
		} else {
			strategy = new FlushEverything(cacheRoot, Collections.EMPTY_LIST);
		}
		strategy.doFlush();
	}
	
	private void internalFlush(String pathAsString, final boolean recursive, boolean suppressEventing) {
		FlushStrategy strategy;
		List<CacheEventListener> listenersToUse;
		cacheEventListenersLock.readLock().lock();
		try {
			listenersToUse = !suppressEventing ? new ArrayList<>(cacheEventListeners) : Collections.EMPTY_LIST;
		} finally {
			cacheEventListenersLock.readLock().unlock();
		}
		if (!recursive) {
			strategy = new FlushPath( pathAsString, cacheRoot, listenersToUse);
		} else {
			strategy = new FlushPathRecursive(pathAsString, cacheRoot, listenersToUse);
		}
		strategy.doFlush();
	}
	
	@Override
	public void link(String entryPath, String linkTargetPath) throws IOException {
		final boolean createMissingTarget = true;
		if (linkTargetPath == null) {
			throw new IllegalArgumentException("linkTargetPath is not allowed to be null");
		}
		if (entryPath == null) {
			throw new IllegalArgumentException("entryPath is not allowed to be null");
		}
		if (skip) {
			LOG.debug("skipping linking of {} and {}", entryPath, linkTargetPath);
			return;
		}
		Path root = Paths.get(getCacheRoot());
		Path linkTarget = convertPathStringToCachePath(linkTargetPath, root);
		if (Files.notExists(linkTarget)) {
			// there is nothing to link to
			// TODO: allow links to missing targets, because this will be proactive
			if (!createMissingTarget) {
				return;
			} else {
				Files.createDirectories(linkTarget);
			}
		}
		Path path = convertPathStringToCachePath(entryPath, root).resolve("links");
		if (Files.notExists(path)) {
			// links to other resources are stored in a separate hidden folder to keep the cache clean
			Files.createDirectories(path);
		}
		Path link = convertPathStringToCachePath(linkTargetPath, path);
		if (Files.notExists(link)) {
			Path linkParent = link.getParent();
			if (Files.notExists(linkParent)) {
				Files.createDirectories(linkParent);
			}
			Path linkTargetRelative = linkParent.relativize(linkTarget);
			if (Files.notExists(link)) {
				Files.createSymbolicLink(link, linkTargetRelative);
			}
		}
	}

	@Override
	public void iterateLinksOf(String entryPath, final Consumer consumer) throws IOException {
		if (consumer == null) {
			return;
		}
		if (entryPath == null) {
			throw new IllegalArgumentException("entryPath is not allowed to be null");
		}
		if (skip) {
			LOG.debug("skipping iteration of links on entry {}", entryPath);
			return;
		}
		Path root = Paths.get(getCacheRoot());
		Path path = convertPathStringToCachePath(entryPath, root).resolve("links");
		if (Files.exists(path) && Files.isDirectory(path)) {
			try (final CacheTransaction cacheTransaction = createCacheTransaction()) {
				try (final CacheTransaction cacheTransactionWithSuppressedEventing = createCacheTransactionWithSuppressedEventing()) {
					Files.walkFileTree(path, new FileVisitor<Path>() {

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							if (attrs.isSymbolicLink()) {
								// is a link
								consumer.accept(createCacheLinkFromPath(dir));
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (attrs.isSymbolicLink()) {
								// is a link
								consumer.accept(createCacheLinkFromPath(file));
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						private CacheLink createCacheLinkFromPath(final Path symbolicLink) {
							return new CacheLink() {

								@Override
								public void invalidate() {
									invalidate(false);
								}

								@Override
								public void invalidateSuppressEventing() {
									invalidate(true);
								}

								private void invalidate(boolean suppressEventing) {
									try {
										Path rp = symbolicLink.toRealPath();
										String linkedCacheEntryPath = Paths.get(getCacheRoot()).toAbsolutePath().relativize(rp).toString();
										// the symbolic link has to be removed first in order to avoid cyclic flushing
										Files.delete(symbolicLink);
										if (suppressEventing) {
											cacheTransactionWithSuppressedEventing.flushRecursive(linkedCacheEntryPath);
										} else {
											cacheTransaction.flushRecursive(linkedCacheEntryPath);
										}
									} catch (IOException e) {
										LOG.error("failed to invalidate cache link. {}", symbolicLink.toString());
									}
								}

							};
						}
					});
				}
			}
		}
	}

	protected static Path convertPathStringToCachePath(String pathAsString, Path cacheRoot) {
		Path path = cacheRoot;
		StringBuffer sb = null;
		for (int index = 0; index < pathAsString.length(); index++) {
			char character = pathAsString.charAt(index);
			if ('/' == character) {
				if (sb != null) {
					path = path.resolve(sb.toString());
					sb = null;
				}
			} else {
				if (sb == null) {
					sb = new StringBuffer();
				}
				sb.append(character);
			}
		}
		if (sb != null) {
			path = path.resolve(sb.toString());
		}
		return path;
	}
	
}
