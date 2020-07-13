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
import org.bndly.rest.api.Context;
import org.bndly.rest.api.HeaderReader;
import org.bndly.rest.api.SecurityHandler;
import org.bndly.rest.api.StatusWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SecurityHandlerImpl implements SecurityHandler {

	private static final Logger LOG = LoggerFactory.getLogger(SecurityHandlerImpl.class);
	private static final String BASIC_HEADER_PREFIX = "Basic ";
	
	private final String realm;
	private final List<SecurityHandler.AuthorizationProvider> authorizationProviders;
	private final Base64Service base64Service;

	public SecurityHandlerImpl(String realm, List<SecurityHandler.AuthorizationProvider> authorizationProviders, Base64Service base64Service) {
		if (realm == null) {
			throw new IllegalArgumentException("realm is not allowed to be null");
		}
		this.realm = realm;
		if (authorizationProviders == null) {
			throw new IllegalArgumentException("authorizationProviders is not allowed to be null");
		}
		this.authorizationProviders = authorizationProviders;
		if (base64Service == null) {
			throw new IllegalArgumentException("base64Service is not allowed to be null");
		}
		this.base64Service = base64Service;
	}

	@Override
	public boolean isServableContext(Context context) {
		HeaderReader hr = context.getHeaderReader();
		String authorizationHeader = hr.read("Authorization");
		if (authorizationHeader == null || !authorizationHeader.startsWith(BASIC_HEADER_PREFIX)) {
			Iterator<AuthorizationProvider> iter = authorizationProviders.iterator();
			while (iter.hasNext()) {
				AuthorizationProvider next = iter.next();
				if (next.isAnonymousAllowed(context)) {
					return true;
				}
			}
			context.getHeaderWriter().write("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
			context.getStatusWriter().write(StatusWriter.Code.UNAUTHORIZED);
			return false;
		} else {
			Iterator<AuthorizationProvider> iter = authorizationProviders.iterator();
			byte[] userAndPw = base64Service.base64Decode(authorizationHeader.substring(BASIC_HEADER_PREFIX.length()));
			try {
				String userAndPwString = new String(userAndPw, "UTF-8");
				int border = userAndPwString.indexOf(":");
				if (border >= 0) {
					String user = userAndPwString.substring(0, border);
					String pw = userAndPwString.substring(border + 1);
					while (iter.hasNext()) {
						AuthorizationProvider next = iter.next();
						if (next.isAuthorized(context, user, pw)) {
							return true;
						}
					}
				}
			} catch (UnsupportedEncodingException ex) {
				LOG.error("could not decode user and password from basic auth header: " + ex.getMessage(), ex);
			}
			context.getStatusWriter().write(StatusWriter.Code.FORBIDDEN);
			return false;
		}
	}

	@Override
	public void invalidateAuthenticationData(Context context) {
	}

}
