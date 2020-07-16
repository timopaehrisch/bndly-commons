package org.bndly.de.rest.jetty.bridge;

/*-
 * #%L
 * REST Jetty Bridge
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

import org.bndly.de.rest.jetty.bridge.impl.BoundFilter;
import org.bndly.de.rest.jetty.bridge.impl.ConditionalIterator;
import org.bndly.de.rest.jetty.bridge.impl.DeployedFilter;
import org.bndly.de.rest.jetty.bridge.impl.DeployedServlet;
import org.bndly.de.rest.jetty.bridge.impl.Remover;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = JettyBridge.class)
public class JettyBridge {

	private static final Logger LOG = LoggerFactory.getLogger(JettyBridge.class);
	@Reference
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private final List<DeployedServlet> deployedServlets = new ArrayList<>();
	private final List<DeployedFilter> deployedFilters = new ArrayList<>();
	private final List<BoundFilter> boundFilters = new ArrayList<>();
	private LifeCycle.Listener lifecyclelistener;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Activate
	public void activate() {
		try {
			lock.writeLock().lock();
			lifecyclelistener = new AbstractLifeCycle.AbstractLifeCycleListener() {

				@Override
				public void lifeCycleFailure(LifeCycle event, Throwable cause) {
					LOG.error("failure in life cycle of jetty server: " + cause.getMessage(), cause);
				}

				@Override
				public void lifeCycleStarted(LifeCycle event) {
					LOG.info("started jetty");
				}

				@Override
				public void lifeCycleStarting(LifeCycle event) {
					LOG.info("starting jetty");
				}

				@Override
				public void lifeCycleStopped(LifeCycle event) {
					LOG.info("stopped jetty");
					if (Server.class.isInstance(event)) {
						event.removeLifeCycleListener(this);
					}
				}

				@Override
				public void lifeCycleStopping(LifeCycle event) {
					LOG.info("stopping jetty");
				}

			};
			server.addLifeCycleListener(lifecyclelistener);
			Handler handler = server.getHandler();
			HandlerCollection hc = (HandlerCollection) handler;
			Handler[] handlers = hc.getHandlers();
			contextHandlerCollection = null;
			if (handlers != null) {
				for (Handler delegatedHandler : handlers) {
					if (ContextHandlerCollection.class.isInstance(delegatedHandler)) {
						contextHandlerCollection = (ContextHandlerCollection) delegatedHandler;
						break;
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Deactivate
	public void deactivate() {
		try {
			lock.writeLock().lock();
			LOG.info("deactivating jetty bridge.");
			if (server.isStopping()) {
				try {
					LOG.info("server is already stopping. waiting for stopping process to complete.");
					server.join();
				} catch (InterruptedException ex) {
					LOG.warn("interrupted while waiting for jetty server to stop.");
				}
			}
			for (BoundFilter boundFilter : boundFilters) {
				try {
					boundFilter.stop();
				} catch (Exception ex) {
					LOG.error("could not stop filter holder: " + ex.getMessage(), ex);
				}
			}
			boundFilters.clear();
			deployedFilters.clear();
			for (DeployedServlet deployedServlet : deployedServlets) {
				stopAndRemoveDeployedServlet(deployedServlet);
			}
			deployedServlets.clear();
			lifecyclelistener = null;
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void stopAndRemoveDeployedServlet(DeployedServlet ds) {
		try {
			ds.stop();
		} catch (Exception ex) {
			LOG.error("could not stop handler: " + ex.getMessage(), ex);
		}
	}

	public void deployServlet(final String servletContextPath, Servlet servlet) {
		try {
			lock.writeLock().lock();
			if (contextHandlerCollection != null) {
				LOG.info("DEPLOYING SERVLET: " + servletContextPath + " " + servlet.getClass().getName());
				final DeployedServlet deployedServlet = new DeployedServlet(contextHandlerCollection, servlet, servletContextPath);
				deployedServlets.add(deployedServlet);
				deployedServlet.start();
				iterate(deployedFilters, new ConditionalIterator<DeployedFilter>() {

					@Override
					public boolean listEntryApplies(DeployedFilter entry) {
						return servletContextPath.startsWith(entry.getContextPath());
					}

					@Override
					public void doWithEntry(DeployedFilter deployedFilter, Remover remover) {
						bindDeployedFilterToDeployedServlet(deployedFilter, deployedServlet);
					}
				});
			} else {
				LOG.error("contextHandlerCollection is null");
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void undeployServlet(final Servlet servlet) {
		iterate(deployedServlets, new ConditionalIterator<DeployedServlet>() {

			@Override
			public boolean listEntryApplies(DeployedServlet ds) {
				boolean r = ds.getServlet() == servlet;
				if (r) {
					LOG.info("UNDEPLOYING SERVLET: " + ds.getServletContextPath() + " " + servlet.getClass().getName());
				}
				return r;
			}

			@Override
			public void doWithEntry(final DeployedServlet ds, Remover remover) {
				unbindFiltersByServlet(ds);
				try {
					ds.stop();
				} catch (Exception ex) {
					LOG.error("failed to stop deployed servlet: " + ex.getMessage(), ex);
				}
				remover.remove();
			}
		});
	}

	public void deployFilter(final String filterContextPath, Filter filter) {
		try {
			lock.writeLock().lock();
			final DeployedFilter deployedFilter = new DeployedFilter(filter, filterContextPath);
			deployedFilters.add(deployedFilter);
			iterate(deployedServlets, new ConditionalIterator<DeployedServlet>() {

				@Override
				public boolean listEntryApplies(DeployedServlet deployedServlet) {
					return deployedServlet.getServletContextPath().startsWith(filterContextPath);
				}

				@Override
				public void doWithEntry(DeployedServlet deployedServlet, Remover remover) {
					bindDeployedFilterToDeployedServlet(deployedFilter, deployedServlet);
				}
			});
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void undeployFilter(final Filter filter) {
		iterate(deployedFilters, new ConditionalIterator<DeployedFilter>() {

			@Override
			public boolean listEntryApplies(DeployedFilter entry) {
				return entry.getFilter() == filter;
			}

			@Override
			public void doWithEntry(final DeployedFilter df, Remover remover) {
				unbindFiltersByFilter(df);
				remover.remove();
			}
		});
	}

	private void bindDeployedFilterToDeployedServlet(DeployedFilter deployedFilter, DeployedServlet deployedServlet) {
		try {
			lock.writeLock().lock();
			BoundFilter boundFilter = new BoundFilter(deployedFilter, deployedServlet);
			try {
				boundFilters.add(boundFilter);
				boundFilter.start();
			} catch (Exception ex) {
				LOG.error("could not start filter holder: " + ex.getMessage(), ex);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void unbindFiltersByServlet(DeployedServlet deployedServlet) {
		try {
			lock.writeLock().lock();
			Iterator<BoundFilter> iterator = boundFilters.iterator();
			while (iterator.hasNext()) {
				BoundFilter next = iterator.next();
				if (next.getServlet() == deployedServlet) {
					iterator.remove();
					try {
						next.stop();
					} catch (Exception e) {
						LOG.error("could not unbind filter from servlet", e);
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void unbindFiltersByFilter(DeployedFilter deployedFilter) {
		try {
			lock.writeLock().lock();
			Iterator<BoundFilter> iterator = boundFilters.iterator();
			while (iterator.hasNext()) {
				BoundFilter next = iterator.next();
				if (next.getFilter() == deployedFilter) {
					iterator.remove();
					try {
						next.stop();
					} catch (Exception e) {
						LOG.error("could not unbind filter from servlet", e);
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private <E> void iterate(List<E> list, ConditionalIterator<E> iteratorCallback) {
		try {
			lock.writeLock().lock();
			final Iterator<E> it = list.iterator();
			while (it.hasNext()) {
				final E element = it.next();
				if (iteratorCallback.listEntryApplies(element)) {
					iteratorCallback.doWithEntry(element, new Remover() {

						@Override
						public void remove() {
							it.remove();
						}
					});
				}

			}
		} finally {
			lock.writeLock().unlock();
		}
	}
}
