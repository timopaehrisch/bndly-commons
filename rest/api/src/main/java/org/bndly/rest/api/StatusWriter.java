package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface StatusWriter {
	public static enum Code {
		OK(200),
		CREATED(201),
		ACCEPTED(202),
		NON_AUTHORITATIVE_INFORMATION(203),
		NO_CONTENT(204),
		RESET_CONTENT(205),
		PARTIAL_CONTENT(206),
		
		MULTIPLE_CHOICES(300),
		MOVED_PERMANENTLY(301),
		FOUND(302),
		SEE_OTHER(303),
		NOT_MODIFIED(304),
		USE_PROXY(305),
		TEMPORARY_REDIRECT(307),
		
		BAD_REQUEST(400),
		UNAUTHORIZED(401),
		PAYMENT_REQUIRED(402),
		FORBIDDEN(403),
		NOT_FOUND(404),
		METHOD_NOT_ALLOWED(405),
		NOT_ACCEPTABLE(406),
		PROXY_AUTHENTICATION_REQUIRED(407),
		REQUEST_TIMEOUT(408),
		CONFLICT(409),
		GONE(410),
		LENGTH_REQUIRED(411),
		PRECONDITION_FAILED(412),
		REQUEST_ENTITY_TOO_LARGE(413),
		REQUEST_URI_TOO_LONG(414),
		UNSUPPORTED_MEDIA_TYPE(415),
		REQUESTED_RANGE_NOT_SATISFIABLE(416),
		EXPECTATION_FAILED(417),
		
		INTERNAL_SERVER_ERROR(500),
		NOT_IMPLEMENTED(501),
		BAD_GATEWAY(502),
		SERVICE_UNAVAILABLE(503),
		GATEWAY_TIMEOUT(504),
		HTTP_VERSION_NOT_SUPPORTED(505)
		;

		private final int httpCode;
		private static final Map<Integer, Code> httpCodeToCode;

		private Code(int httpCode) {
			this.httpCode = httpCode;
		}

		public final int getHttpCode() {
			return httpCode;
		}
		
		static {
			httpCodeToCode = new HashMap<>();
			for (Code value : Code.values()) {
				httpCodeToCode.put(value.getHttpCode(), value);
			}
		}
		
		public static Code fromHttpCode(int httpCode)  {
			return httpCodeToCode.get(httpCode);
		}
	}
	
	StatusWriter write(Code code);
}
