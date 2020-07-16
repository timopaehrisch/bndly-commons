package org.bndly.rest.schema.websocket;

/*-
 * #%L
 * REST Schema Websocket
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
import org.bndly.de.rest.websocket.api.EventHolder;
import org.bndly.de.rest.websocket.api.Socket;
import org.bndly.de.rest.websocket.api.WebSocketEventListener;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Schema;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
@Component(service = WebSocketEventListener.class)
public class SchemaWebSocketEventListener implements WebSocketEventListener, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(SchemaWebSocketEventListener.class);
	private final List<EngineRelatedListener> engines = new ArrayList<>();
	private final ReadWriteLock enginesLock = new ReentrantReadWriteLock();
	private final List<Socket> knownSockets = new ArrayList<>();
	private final ReadWriteLock knownSocketsLock = new ReentrantReadWriteLock();
	private final Map<String, List<EntityData>> eventQueueData = new HashMap<>();
	private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
	
	@Activate
	public void activate() {
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		scheduledThreadPoolExecutor.scheduleAtFixedRate(this, 0, 4, TimeUnit.SECONDS);
	}
	
	@Deactivate
	public void deactivate() {
		enginesLock.writeLock().lock();
		try {
			for (EngineRelatedListener engine : engines) {
				engine.unbindFromEngine();
			}
			engines.clear();
		} finally {
			enginesLock.writeLock().unlock();
		}
		dealWithQueue(new QueueCallback() {

			@Override
			public void doWithQueue(Map<String, List<EntityData>> eventQueueData) {
				eventQueueData.clear();
			}
		});
		knownSocketsLock.writeLock().lock();
		try {
			knownSockets.clear();
		} finally {
			knownSocketsLock.writeLock().unlock();
		}
		scheduledThreadPoolExecutor.shutdown();
	}
	
	@Reference(
			bind = "addEngine",
			unbind = "removeEngine",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = Engine.class
	)
	public void addEngine(Engine engine) {
		if (engine != null) {
			enginesLock.writeLock().lock();
			try {
				EngineRelatedListener l = new EngineRelatedListener(engine, this);
				engines.add(l);
				l.bindToEngine();
			} finally {
				enginesLock.writeLock().unlock();
			}
		}
	}

	public void removeEngine(Engine engine) {
		if (engine != null) {
			enginesLock.writeLock().lock();
			try {
				List<EngineRelatedListener> toRemove = null;
				for (EngineRelatedListener toRemove1 : engines) {
					if (toRemove1.getEngine() == engine) {
						if (toRemove == null) {
							toRemove = new ArrayList<>();
						}
						toRemove.add(toRemove1);
						toRemove1.unbindFromEngine();
					}
				}
				if (toRemove != null) {
					engines.removeAll(toRemove);
				}
			} finally {
				enginesLock.writeLock().unlock();
			}
		}
	}
	
	public static interface Callback {
		void run(List<Socket> sockets);
	}
	
	public void doWithKnownSockets(Callback callback){
		knownSocketsLock.readLock().lock();
		try {
			callback.run(knownSockets);
		} finally {
			knownSocketsLock.readLock().unlock();
		}
	}
	
	private List<Socket> getKnownSockets() {
		return knownSockets;
	}
	
	@Override
	public void onWebSocketBinary(Socket socket, byte[] bytes, int i, int i1) {
		
	}

	@Override
	public void onWebSocketClose(Socket socket, int i, String string) {
		knownSocketsLock.writeLock().lock();
		try {
			knownSockets.remove(socket);
		} finally {
			knownSocketsLock.writeLock().unlock();
		}
	}

	@Override
	public void onWebSocketConnect(Socket socket) {
		knownSocketsLock.writeLock().lock();
		try {
			knownSockets.add(socket);
		} finally {
			knownSocketsLock.writeLock().unlock();
		}
	}

	@Override
	public void onWebSocketError(Socket socket, Throwable throwable) {
	}

	@Override
	public void onWebSocketText(Socket socket, String string) {
	}

	public void queueEvent(EntityData ed, EngineRelatedListener listener) {
		Schema ds = listener.getEngine().getDeployer().getDeployedSchema();
		if (ds != null) {
			String ns = ds.getNamespace();
			addEventDataToQueue(ns, ed);
		}
	}

	private void addEventDataToQueue(final String ns, final EntityData ed) {
		dealWithQueue(new QueueCallback() {

			@Override
			public void doWithQueue(Map<String, List<EntityData>> eventQueueData) {
				List<EntityData> l = eventQueueData.get(ns);
				if (l == null) {
					l = new ArrayList<>();
					eventQueueData.put(ns, l);
				}
				l.add(ed);
			}
		});
	}
	
	private synchronized void dealWithQueue(QueueCallback callback) {
		callback.doWithQueue(eventQueueData);
	}

	@Override
	public void run() {
		dealWithQueue(new QueueCallback() {

			@Override
			public void doWithQueue(Map<String, List<EntityData>> eventQueueData) {
				if (eventQueueData.isEmpty()) {
					return;
				}
				List<Socket> defCopy = new ArrayList<>(getKnownSockets());
				try {
					for (Map.Entry<String, List<EntityData>> entrySet : eventQueueData.entrySet()) {
						String nameSpace = entrySet.getKey();
						List<EntityData> value = entrySet.getValue();
						SchemaEventData d = new SchemaEventData();
						d.setNamespace(nameSpace);
						d.setEntities(value);
						EventHolder holder = buildEventHolder(d);
						JSValue marshalled = new Marshaller().marshall(holder);
						StringWriter sw = new StringWriter();
						try {
							new JSONSerializer().serialize(marshalled, sw);
							sw.flush();
						} catch (IOException ex) {
							LOG.error("could not serialize event data",ex);
							continue;
						}
						String message = sw.toString();
						for (Socket socket : defCopy) {
							try {
								socket.sendMessage(message);
							} catch (IOException ex) {
								LOG.error("could not send message via socket",ex);
							}
						}
					}
				} finally {
					eventQueueData.clear();
				}
			}
		});
	}
	
	public EventHolder buildEventHolder(Object data) {
		return EventHolder.create("schema", "entityModification", data);
	}

	private static interface QueueCallback {
		void doWithQueue(Map<String, List<EntityData>> eventQueueData);
	}
}
