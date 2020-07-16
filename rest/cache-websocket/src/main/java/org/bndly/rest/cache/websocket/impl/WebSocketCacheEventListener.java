package org.bndly.rest.cache.websocket.impl;

/*-
 * #%L
 * REST Cache Websocket
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
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.parsing.ParsingException;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.de.rest.websocket.api.Event;
import org.bndly.de.rest.websocket.api.EventHolder;
import org.bndly.de.rest.websocket.api.Socket;
import org.bndly.de.rest.websocket.api.WebSocketEventListener;
import org.bndly.rest.cache.api.CacheEventListener;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.cache.api.CacheTransactionListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
@Component(service = { CacheTransactionListener.class, WebSocketEventListener.class })
public class WebSocketCacheEventListener implements WebSocketEventListener, CacheTransactionListener {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketCacheEventListener.class);

	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	private final List<Socket> knownSockets = new ArrayList<>();

	@Activate
	public void activate() {
		cacheTransactionFactory.addCacheTransactionListener(this);
	}
	
	@Deactivate
	public void deactivate() {
		cacheTransactionFactory.removeCacheTransactionListener(this);
	}
	
	@Override
	public void onCommit(CacheTransaction cacheTransaction) {
		// collect all cache events from the transaction
		// these events will then be put in a websocket event message
		final TransactionCommitEventData transactionCommitEventData = new TransactionCommitEventData();
		cacheTransaction.replayEvents(new CacheEventListener() {

			@Override
			public void onFlush() {
				transactionCommitEventData.setComplete(true);
			}

			@Override
			public void onFlush(String pathAsString, boolean recursive) {
				if (recursive) {
					transactionCommitEventData.addRecursivePath(pathAsString);
				} else {
					transactionCommitEventData.addPath(pathAsString);
				}
			}

			@Override
			public void onLink(String entryPath, String linkTargetPath) {
				// TODO
			}
		});
		fireWebsocketEvent("commit", transactionCommitEventData);
	}

	public void onFlush() {
		final FlushEventData flushEventData = FlushEventData.complete();
		fireWebsocketFlushEvent(flushEventData);
	}

	private void fireWebsocketFlushEvent(FlushEventData flushEventData) {
		fireWebsocketEvent("flushed", flushEventData);
	}
	
	private void fireWebsocketLinkEvent(LinkEventData linkEventData) {
		fireWebsocketEvent("linked", linkEventData);
	}
	
	private void fireWebsocketEvent(String eventName, Object data) {
		EventHolder eventHolder = EventHolder.create("cache", eventName, data);
		JSValue marhsalled = new Marshaller().marshall(eventHolder);
		StringWriter sw = new StringWriter();
		String eventDataJson = null;
		try {
			new JSONSerializer().serialize(marhsalled, sw);
			sw.flush();
			eventDataJson = sw.toString();
		} catch (IOException ex) {
			LOG.error("failed to serialize the event holder for a cache event: " + ex.getMessage(), ex);
		}
		if (eventDataJson != null) {
			sendEventData(eventDataJson);
		}
	}

	public void onLink(String entryPath, String linkTargetPath) {
		fireWebsocketLinkEvent(new LinkEventData(entryPath, linkTargetPath));
	}

	public void onFlush(String pathAsString, boolean recursive) {
		final FlushEventData flushEventData;
		if (recursive) {
			flushEventData = FlushEventData.pathRecursive(pathAsString);
		} else {
			flushEventData = FlushEventData.path(pathAsString);
		}
		fireWebsocketFlushEvent(flushEventData);
	}

	@Override
	public void onWebSocketBinary(Socket socket, byte[] bytes, int i, int i1) {
	}

	@Override
	public void onWebSocketClose(Socket socket, int i, String string) {
		knownSockets.remove(socket);
	}

	@Override
	public void onWebSocketConnect(Socket socket) {
		this.knownSockets.add(socket);
	}

	@Override
	public void onWebSocketError(Socket socket, Throwable throwable) {
	}

	@Override
	public void onWebSocketText(Socket socket, String string) {
		try {
			JSObject jsonDoc = (JSObject) new JSONParser().parse(new StringReader(string));
			if (jsonDoc == null) {
				return;
			}
			JSObject eventDescription = jsonDoc.getMemberValue("event", JSObject.class);
			if (eventDescription == null) {
				return;
			}
			String namespace = eventDescription.getMemberStringValue("namespace");
			String name = eventDescription.getMemberStringValue("name");
			Event event = null;
			if (name != null && namespace != null) {
				event = new Event(namespace, name);
			}
			if (event != null) {
				if ("cache".equals(event.getNamespace()) && "flushed".equals(event.getName())) {
					JSObject dataObject = jsonDoc.getMemberValue("data", JSObject.class);
					if (dataObject != null) {
						String transaction = dataObject.getMemberStringValue("transaction");
						String path = dataObject.getMemberStringValue("path");
						Boolean recursive = dataObject.getMemberBooleanValue("recursive");
						Boolean complete = dataObject.getMemberBooleanValue("complete");
						if (transaction != null && ((path != null && recursive != null) || complete != null)) {
							try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransactionWithSuppressedEventing()) {
								FlushEventData eventData;
								if (complete != null && complete) {
									eventData = FlushEventData.complete();
								} else {
									if (recursive != null && recursive) {
										eventData = FlushEventData.pathRecursive(path);
									} else {
										eventData = FlushEventData.path(path);
									}
								}
								onRemoteFlushEvent(eventData, cacheTransaction);
							}
						}
					}
				}
			}
		} catch (ParsingException e) {
			// then someone sent a message we don't understand and don't care about.
		}
	}

	private void onRemoteFlushEvent(FlushEventData eventData, CacheTransaction cacheTransaction) {
		if (eventData != null) {
			if (eventData.isComplete()) {
				cacheTransaction.flush();
			} else {
				if (eventData.getPath() != null) {
					if (eventData.isRecursive()) {
						cacheTransaction.flushRecursive(eventData.getPath());
					} else {
						cacheTransaction.flush(eventData.getPath());
					}
				}
			}
		}
	}

	private void sendEventData(String eventDataJson) {
		if (eventDataJson == null) {
			return;
		}
		for (Socket knownSocket : knownSockets) {
			try {
				knownSocket.sendMessage(eventDataJson);
			} catch (IOException ex) {
				LOG.error("failed to send event data json to socket: " + ex.getMessage(), ex);
			}
		}
	}

}
