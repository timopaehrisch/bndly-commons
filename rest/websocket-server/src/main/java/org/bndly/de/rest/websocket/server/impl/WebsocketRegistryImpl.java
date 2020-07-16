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

import org.bndly.de.rest.jetty.bridge.JettyBridge;
import org.bndly.de.rest.websocket.api.DelegatingWebSocketEventListener;
import org.bndly.de.rest.websocket.api.WebSocketEventListener;
import org.bndly.de.rest.websocket.api.WebSocketListHandler;
import org.bndly.de.rest.websocket.api.WebSocketRegistry;
import org.bndly.de.rest.websocket.api.WebsocketEventDispatcher;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Servlet;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
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
@Component(service = WebSocketRegistry.class, immediate = true)
public class WebsocketRegistryImpl implements WebSocketRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(WebsocketRegistryImpl.class);
	@Reference
	private JettyBridge jettyBridge;

	private final DelegatingWebSocketEventListener eventListener = new DelegatingWebSocketEventListener();
	private final List<WebsocketEventDispatcher> dispatchers = new ArrayList<>();
	private Servlet servlet;

	@Activate
	public void activate() {
		servlet = new WebSocketServlet() {

			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.setCreator(new WebSocketCreator() {

					@Override
					public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
						return createWebSocketDispatcher();
					}

				});
			}
		};
		jettyBridge.deployServlet("/websocket", servlet);
	}

	@Deactivate
	public void deactivate() {
		doWithActiveSockets(new WebSocketListHandler() {

			@Override
			public void handle(List<WebsocketEventDispatcher> dispatchers) {
				for (WebsocketEventDispatcher dispatcher : dispatchers) {
					dispatcher.close();
				}
			}
		});
		try {
			jettyBridge.undeployServlet(servlet);
		} catch (Exception ex) {
			LOG.error("failed to stop websocketContextHandler: " + ex.getMessage(), ex);
		} finally {
			servlet = null;
		}
	}

	private WebsocketEventDispatcher createWebSocketDispatcher() {
		final WebsocketEventDispatcher dispatcher = new JettyWebsocketEventDispatcher() {

			@Override
			public WebSocketEventListener getWebSocketEventListener() {
				return eventListener;
			}

			@Override
			public void close() {
				try {
					super.close();
				} finally {
					dispatchers.remove(this);
				}
			}
			
		};
		dispatchers.add(dispatcher);
		return dispatcher;
	}
	
	@Reference(
			bind = "addListener",
			unbind = "removeListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = WebSocketEventListener.class
	)
	@Override
	public void addListener(WebSocketEventListener listener) {
		eventListener.addListener(listener);
	}

	@Override
	public void removeListener(WebSocketEventListener listener) {
		eventListener.removeListener(listener);
	}

	@Override
	public void doWithActiveSockets(WebSocketListHandler callback) {
		List<WebsocketEventDispatcher> defensiveCopy = new ArrayList<>(dispatchers);
		callback.handle(defensiveCopy);
	}
}
