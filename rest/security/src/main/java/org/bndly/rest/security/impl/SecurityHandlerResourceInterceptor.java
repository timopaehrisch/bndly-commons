package org.bndly.rest.security.impl;

/*-
 * #%L
 * REST Security
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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceInterceptor;
import org.bndly.rest.api.SecurityHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = { ResourceInterceptor.class })
@Designate(ocd = SecurityHandlerResourceInterceptor.Configuration.class)
public class SecurityHandlerResourceInterceptor implements ResourceInterceptor, WebConsoleSecurityProvider {

	private ServiceRegistration<WebConsoleSecurityProvider> reg;

	@ObjectClassDefinition(
			name = "Security Resource Interceptor"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Realm",
				description = "The realm to use for the authentication"
		)
		String realm() default "bndly";

		@AttributeDefinition(
				name = "Disabled",
				description = "If disabled, any resource can be accessed."
		)
		boolean disabled() default false;

		@AttributeDefinition(
				name = "Is WebConsole Security Provider",
				description = "If disabled, this component will not manage security of the WebConsole"
		)
		boolean actAsWebConsoleSecurityProvider() default true;

	}
	
	private String realm;
	private boolean disabled;
	
	private final List<SecurityHandler.AuthorizationProvider> authorizationProviders = new ArrayList<>();
	private final ReadWriteLock authorizationProvidersLock = new ReentrantReadWriteLock();
	private List<SecurityHandler.AuthorizationProvider> immutableAuthorizationProviders = Collections.EMPTY_LIST;
	
	@Reference
	private Base64Service base64Service;

	@Activate
	public void activate(Configuration configuration, BundleContext bundleContext) {
		realm = configuration.realm();
		disabled = configuration.disabled();
		if (disabled) {
			addAuthorizationProvider(new SecurityHandler.AuthorizationProvider() {

				@Override
				public boolean isAnonymousAllowed(Context context) {
					return true;
				}

				@Override
				public boolean isAuthorized(Context context, String user, String password) {
					return true;
				}
			});
		}
		if (configuration.actAsWebConsoleSecurityProvider()) {
			reg = ServiceRegistrationBuilder.newInstance(WebConsoleSecurityProvider.class, this).register(bundleContext);
		}
	}

	@Deactivate
	public void deactivate() {
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}

	@Reference(
			bind = "addAuthorizationProvider",
			unbind = "removeAuthorizationProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = SecurityHandler.AuthorizationProvider.class
	)
	public void addAuthorizationProvider(SecurityHandler.AuthorizationProvider provider) {
		if (provider != null) {
			authorizationProvidersLock.writeLock().lock();
			try {
				authorizationProviders.add(provider);
				immutableAuthorizationProviders = Collections.unmodifiableList(authorizationProviders);
			} finally {
				authorizationProvidersLock.writeLock().unlock();
			}
		}
	}
	
	public void removeAuthorizationProvider(SecurityHandler.AuthorizationProvider provider) {
		if (provider != null) {
			authorizationProvidersLock.writeLock().lock();
			try {
				Iterator<SecurityHandler.AuthorizationProvider> iter = authorizationProviders.iterator();
				while (iter.hasNext()) {
					SecurityHandler.AuthorizationProvider next = iter.next();
					if (next == provider) {
						iter.remove();
					}
				}
				immutableAuthorizationProviders = Collections.unmodifiableList(authorizationProviders);
			} finally {
				authorizationProvidersLock.writeLock().unlock();
			}
		}
	}
	
	@Override
	public void beforeResourceResolving(Context context) {
		SecurityHandlerImpl securityHandlerImpl = new SecurityHandlerImpl(realm, immutableAuthorizationProviders, base64Service);
		context.setSecurityHandler(securityHandlerImpl);
	}

	@Override
	public Resource intercept(Resource input) {
		return input;
	}

	@Override
	public void doFinally(Context context) {
	}

	@Override
	public Object authenticate(String user, String password) {
		for (SecurityHandler.AuthorizationProvider authorizationProvider : immutableAuthorizationProviders) {
			if (authorizationProvider.isAuthorized(null, user, password)) {
				return user;
			}
		}
		return null;
	}

	@Override
	public boolean authorize(Object user, String role) {
		return true;
	}
	
}
