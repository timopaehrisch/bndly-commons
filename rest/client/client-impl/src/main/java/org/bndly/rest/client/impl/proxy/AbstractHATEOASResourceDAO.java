package org.bndly.rest.client.impl.proxy;

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

import org.bndly.rest.client.api.ClientConfiguration;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.HATEOASClientFactory;
import org.bndly.rest.client.api.RESTResource;
import org.bndly.rest.client.exception.MissingLinkClientException;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Deprecated
public abstract class AbstractHATEOASResourceDAO<RootResourceBeanT> {

	private HATEOASClient<RootResourceBeanT> rootResourceClient;
	private HATEOASClientFactory clientFactory;
	private ClientConfiguration clientConfiguration;
	private Object subResource;
	private HATEOASClient<Object> subResourceClient;
	private boolean missingLinkDuringInit;
	private Thread initThread;
	private Exception initExecption;

	@PostConstruct
	public void init() {
		initThread = createInitThread();
		initThread.start();
	}

	private Thread createInitThread() {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				loadPrimaryResource();
			}
		}, getClass().getSimpleName() + " Init Thread");
	}

	private void loadPrimaryResource() {
		String hostUrlInternal = clientConfiguration.getHostUrl();
		if (!hostUrlInternal.endsWith("/")) {
			hostUrlInternal = hostUrlInternal + "/";
		}
		final String hostUrl = hostUrlInternal;
		try {
			// load the root resource to see what resources the REST service can offer us
			RootResourceBeanT services = createRootResourceStubWithEntryURL(hostUrl);
			rootResourceClient = clientFactory.build(services);
			services = rootResourceClient.read().execute();

			// with the loaded root resource, we will try to load our favored subresource
			// we will recognize this subresource by following a specific link (this is the contract between client and server)
			rootResourceClient = clientFactory.build(services);
			RESTResource resourceDescriptor = getResourceDescriptor();
			subResource = rootResourceClient.follow(resourceDescriptor.rel()).execute(resourceDescriptor.type());
			subResourceClient = clientFactory.build(subResource);
		} catch (MissingLinkClientException e) {
			// the dao won't be able to operate even we go a connection
			missingLinkDuringInit = true;
		} catch (Exception e) {
			this.initExecption = e;
			throw new IllegalStateException("unhandled excpetion while initializing: " + e.getMessage(), e);
		}
	}

	protected abstract RootResourceBeanT createRootResourceStubWithEntryURL(String hostUrl);

	protected <E> E getSubResourceAs() {
		throwExceptionIfInitFailed();
		if (subResource == null) {
			String msg = "finished initialization without obtaining a sub resource. " + getClass().getSimpleName();
			if (initExecption != null) {
				throw new IllegalStateException(msg + ": " + initExecption.getMessage(), initExecption);
			} else {
				throw new IllegalStateException(msg);
			}
		}
		return (E) subResource;
	}

	protected <E> HATEOASClient<E> getSubResourceClientAs() {
		throwExceptionIfInitFailed();
		if (subResourceClient == null) {
			String msg = "finished initialization without obtaining a sub resource. " + getClass().getSimpleName();
			if (initExecption != null) {
				throw new IllegalStateException(msg + ": " + initExecption.getMessage(), initExecption);
			} else {
				throw new IllegalStateException(msg);
			}
		}
		return (HATEOASClient<E>) subResourceClient;
	}

	protected <E> HATEOASClient<E> createClient(E resource) {
		return clientFactory.build(resource);
	}

	@Resource(name = ClientConfiguration.NAME)
	public void setClientConfiguration(ClientConfiguration configuration) {
		this.clientConfiguration = configuration;
	}

	@Resource(name = HATEOASClientFactory.NAME)
	public void setClientFactory(HATEOASClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	private void throwExceptionIfInitFailed() {
		try {
			initThread.join();
		} catch (InterruptedException ex) {
			throw new IllegalStateException("could not join with init thread for ResourceDAO: " + ex.getMessage(), ex);
		}
		if (missingLinkDuringInit) {
			RESTResource descriptor = getResourceDescriptor();
			throw new IllegalStateException(getClass().getSimpleName() + " could not be initialized due to a missing link the service. link name was '" + descriptor.rel() + "'");
		}
	}

	private RESTResource getResourceDescriptor() throws IllegalStateException {
		RESTResource resourceDescriptor = getClass().getAnnotation(RESTResource.class);
		if (resourceDescriptor == null) {
			throw new IllegalStateException("could not find " + RESTResource.class.getSimpleName() + " annotation on " + getClass().getSimpleName());
		}
		return resourceDescriptor;
	}
}
