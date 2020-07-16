package org.bndly.rest.client.impl.hateoas;

/*-
 * #%L
 * REST Client Impl
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

import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.atomlink.api.annotation.AtomLinkHolder;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.HATEOASClientFactory;
import org.bndly.rest.client.api.ServiceFactory;
import org.bndly.rest.client.exception.ClientException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceFactoryImpl implements ServiceFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceFactoryImpl.class);
	private HATEOASClientFactory clientFactory;
	private Object rootResource;
	private String hostUrl;
	private boolean isInitialized = false;
	private int retries = 10;
	private int sleepIntervalSeconds = 5;
	private ExecutorService executorService;
	private Exception initThreadException;
	private Future<?> initFuture;

	public Future<?> init() {
		if (executorService == null) {
			executorService = Executors.newSingleThreadExecutor();
		}
		Runnable initRunnable = new Runnable() {
			@Override
			public void run() {
				Field f = ReflectionUtil.getFieldByAnnotation(AtomLinkHolder.class, rootResource);
				List<AtomLinkBean> links = new ArrayList<>();
				links.add(new AtomLinkBean() {

					@Override
					public String getRel() {
						return "self";
					}

					@Override
					public void setRel(String rel) {
					}

					@Override
					public String getHref() {
						return hostUrl;
					}

					@Override
					public void setHref(String href) {
					}

					@Override
					public String getMethod() {
						return "GET";
					}

					@Override
					public void setMethod(String method) {
					}
				});
				ReflectionUtil.setFieldValue(f, links, rootResource);

				while (!isInitialized && retries >= 0) {
					try {
						HATEOASClient client = clientFactory.build(rootResource);
						rootResource = client.read().execute();
						isInitialized = true;
					} catch (Exception e) {
						LOG.error("init of service factory failed. " + retries + " retries left: " + e.getMessage(), e);
						retries--;
						initThreadException = e;
						try {
							Thread.sleep(sleepIntervalSeconds * 1000);
						} catch (InterruptedException ex) {
							// if we are interrupted, we cancel the initialization
							LOG.warn("initialization has been cancelled");
							break;
						}
					}
				}
			}
		};
		Future<?> initFutureTmp = executorService.submit(initRunnable);
		this.initFuture = initFutureTmp;
		return initFutureTmp;
	}
	
	public void destroy() {
		if (executorService != null) {
			LOG.info("shutting down service factory now.");
			executorService.shutdownNow();
			executorService = null;
		}
	}

	private Object getInitializedRootResource() throws ClientException {
		if (initFuture == null) {
			throw new ClientException("initialization had not been started");
		}
		try {
			// join
			initFuture.get();
			if (isInitialized) {
				return rootResource;
			} else {
				if (RuntimeException.class.isInstance(initThreadException)) {
					throw ((RuntimeException) initThreadException);
				} else {
					throw new ClientException("initialization failed: " + initThreadException.getMessage(), initThreadException);
				}
			}
		} catch (InterruptedException ex) {
			LOG.warn("interrupted while waiting for root resource.");
			throw new ClientException("initialization had been aborted");
		} catch (ExecutionException ex) {
			LOG.error("loading of root resource failed: " + ex.getMessage(), ex);
			throw new ClientException("initialization failed: " + ex.getMessage(), ex);
		}
	}

	@Override
	public <E> HATEOASClient<E> getServiceClient(String linkName, Class<E> responseType) throws ClientException {
		E r = createClient(getInitializedRootResource()).follow(linkName).execute(responseType);
		return createClient(r);
	}

	@Override
	public <E> HATEOASClient<E> createClient(E bean) {
		return clientFactory.build(bean);
	}

	public void setRootResource(Object rootResource) {
		this.rootResource = rootResource;
	}

	public void setClientFactory(HATEOASClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public void setSleepIntervalSeconds(int sleepIntervalSeconds) {
		this.sleepIntervalSeconds = sleepIntervalSeconds;
	}
	

}
