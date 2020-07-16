package org.bndly.de.rest.websocket.client.impl;

/*-
 * #%L
 * REST Websocket Client
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
import org.bndly.de.rest.websocket.api.WebsocketClientConnectionFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = WebsocketClientConnectionFactory.class, immediate = true)
public class WebsocketClientConnectionFactoryImpl implements WebsocketClientConnectionFactory {

	private static final Logger LOG = LoggerFactory.getLogger(WebsocketClientConnectionFactoryImpl.class);

	private final WebSocketClient client = new WebSocketClient();
	private boolean didStart;

	@Activate
	public void activate() {
		try {
			client.start();
			didStart = true;
		} catch (Exception ex) {
			LOG.error("failed to start websocket client: " + ex.getMessage(), ex);
		}
	}

	@Deactivate
	public void deactivate() {
		if (didStart) {
			didStart = false;
			try {
				client.stop();
			} catch (Exception ex) {
				LOG.error("failed to stop websocket client: " + ex.getMessage(), ex);
			}
		} else {
			LOG.warn("shutting down, but client had never started");
		}
	}

	@Override
	public Socket connectTo(String destinationUri, final WebSocketEventListener callback) throws URISyntaxException {
		final ClientSocket socket = new ClientSocket();
		URI uri = new URI(destinationUri);
		ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
		try {
			final Future<Session> session = client.connect(new WebSocketListener() {

				@Override
				public void onWebSocketBinary(byte[] bytes, int i, int i1) {
					callback.onWebSocketBinary(socket, bytes, i, i1);
				}

				@Override
				public void onWebSocketClose(int i, String string) {
					callback.onWebSocketClose(socket, i, string);
				}

				@Override
				public void onWebSocketConnect(Session sn) {
					callback.onWebSocketConnect(socket);
				}

				@Override
				public void onWebSocketError(Throwable thrwbl) {
					callback.onWebSocketError(socket, thrwbl);
				}

				@Override
				public void onWebSocketText(String string) {
					callback.onWebSocketText(socket, string);
				}
			}, uri, upgradeRequest);
			socket.setSession(session);
		} catch (IOException e) {
			LOG.error("could not connect to socket: " + e.getMessage(), e);
		}
		return socket;
	}

}
