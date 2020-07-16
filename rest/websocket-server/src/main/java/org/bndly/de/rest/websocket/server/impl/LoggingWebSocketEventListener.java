package org.bndly.de.rest.websocket.server.impl;

/*-
 * #%L
 * REST Websocket Server
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
import org.bndly.de.rest.websocket.api.WebSocketEventListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = WebSocketEventListener.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = LoggingWebSocketEventListener.Configuration.class)
public class LoggingWebSocketEventListener implements WebSocketEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(LoggingWebSocketEventListener.class);

	@ObjectClassDefinition(
			name = "WebSocket Event Logging Listener",
			description = "This listener will react on all incoming WebSocket events and will log them with INFO level. This listener is intended for development purpose."
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Enable Logging",
				description = "If set to true, the logging will be enabled."
		)
		boolean enableLog() default false;
	}

	private Boolean enableLog;
	
	@Activate
	public void activate(Configuration configuration) {
		enableLog = configuration.enableLog();
	}
	
	@Override
	public void onWebSocketBinary(Socket socket, byte[] bytes, int i, int i1) {
		if (enableLog) {
			LOG.info("websocket binary data received");
		}
	}

	@Override
	public void onWebSocketClose(Socket socket, int i, String string) {
		if (enableLog) {
			LOG.info("websocket closed");
		}
	}

	@Override
	public void onWebSocketConnect(Socket socket) {
		if (enableLog) {
			LOG.info("websocket connected");
		}
	}

	@Override
	public void onWebSocketError(Socket socket, Throwable throwable) {
		if (enableLog) {
			LOG.info("websocket error");
		}
	}

	@Override
	public void onWebSocketText(Socket socket, String string) {
		if (enableLog) {
			LOG.info("websocket text received: {}", string);
		}
	}
	
}
