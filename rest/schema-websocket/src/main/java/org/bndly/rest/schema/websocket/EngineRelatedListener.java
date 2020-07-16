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

import org.bndly.de.rest.websocket.api.EventHolder;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.api.services.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class EngineRelatedListener implements PersistListener, MergeListener, DeleteListener {

	private static final Logger LOG = LoggerFactory.getLogger(EngineRelatedListener.class);
	private final Engine engine;
	private final SchemaWebSocketEventListener socketEventListener;

	public EngineRelatedListener(Engine engine, SchemaWebSocketEventListener socketEventListener) {
		this.engine = engine;
		this.socketEventListener = socketEventListener;
	}

	public Engine getEngine() {
		return engine;
	}

	@Override
	public void onPersist(Record record) {
		EntityData ed = buildEntityData(EntityData.MODIFICATION_CREATED, record);
		socketEventListener.queueEvent(ed, this);
	}

	@Override
	public void onMerge(Record record) {
		EntityData ed = buildEntityData(EntityData.MODIFICATION_UPDATED, record);
		socketEventListener.queueEvent(ed, this);
	}

	@Override
	public void onDelete(Record record) {
		EntityData ed = buildEntityData(EntityData.MODIFICATION_DELETED, record);
		socketEventListener.queueEvent(ed, this);
	}
	
	public void bindToEngine() {
		engine.addListener(this);
	}

	public void unbindFromEngine() {
		engine.removeListener(this);
	}

	private EntityData buildEntityData(String modification, Record record) {
		EntityData ed = new EntityData();
		ed.setModification(modification);
		ed.setType(record.getType().getName());
		ed.setId(record.getId());
		return ed;
	}

}
