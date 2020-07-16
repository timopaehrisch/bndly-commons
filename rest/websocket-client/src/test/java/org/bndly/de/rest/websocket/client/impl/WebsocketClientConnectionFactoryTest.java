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

import org.bndly.common.json.marshalling.Marshaller;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.de.rest.websocket.api.DelegatingWebSocketEventListener;
import org.bndly.de.rest.websocket.api.EmptyWebSocketEventListener;
import org.bndly.de.rest.websocket.api.EventHolder;
import org.bndly.de.rest.websocket.api.Socket;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class WebsocketClientConnectionFactoryTest {

//	@Test
	public void testConnect() throws URISyntaxException, IOException, InterruptedException {
		WebsocketClientConnectionFactoryImpl f = new WebsocketClientConnectionFactoryImpl();
		try {
			f.activate();
			DelegatingWebSocketEventListener delegate = new DelegatingWebSocketEventListener();
			Socket socket = f.connectTo("ws://localhost:8082", delegate);
			delegate.addListener(new EmptyWebSocketEventListener() {

				@Override
				public void onWebSocketText(Socket socket, String string) {
					System.out.println(string);
				}

			});
			// this has to be hooked in the cache invalidation service.
			FlushEventData flushEventData = new FlushEventData("/ebx/Cart/1", false);
			EventHolder holder = EventHolder.create("cache", "flushed", flushEventData);
			JSValue marshalled = new Marshaller().marshall(holder);
			StringWriter sw = new StringWriter();
			new JSONSerializer().serialize(marshalled, sw);
			sw.flush();
			socket.sendMessage(sw.toString());
			Thread.sleep(3000);
			socket.close();
		} finally {
			f.deactivate();
		}
	}
}
