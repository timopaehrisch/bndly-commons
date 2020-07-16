package org.bndly.schema.impl.factory;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.api.listener.PreDeleteListener;
import org.bndly.schema.api.listener.PreMergeListener;
import org.bndly.schema.api.listener.PrePersistListener;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.listener.TransactionListener;
import org.bndly.schema.api.services.Engine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ListenerTracker extends ServiceTracker {

	private static final String COND_LISTENER_DEPLOYMENT = "(" + Constants.OBJECTCLASS + "=" + SchemaDeploymentListener.class.getName() + ")";
	private static final String COND_LISTENER_TRANSACTION = "(" + Constants.OBJECTCLASS + "=" + TransactionListener.class.getName() + ")";
	private static final String COND_LISTENER_PRE_MERGE = "(" + Constants.OBJECTCLASS + "=" + PreMergeListener.class.getName() + ")";
	private static final String COND_LISTENER_MERGE = "(" + Constants.OBJECTCLASS + "=" + MergeListener.class.getName() + ")";
	private static final String COND_LISTENER_PRE_PERSIST = "(" + Constants.OBJECTCLASS + "=" + PrePersistListener.class.getName() + ")";
	private static final String COND_LISTENER_PERSIST = "(" + Constants.OBJECTCLASS + "=" + PersistListener.class.getName() + ")";
	private static final String COND_LISTENER_PRE_DELETE = "(" + Constants.OBJECTCLASS + "=" + PreDeleteListener.class.getName() + ")";
	private static final String COND_LISTENER_DELETE = "(" + Constants.OBJECTCLASS + "=" + DeleteListener.class.getName() + ")";
	private final Engine engine;
	private final List<Object> listeners = new ArrayList<>();
	private final Lock lock = new ReentrantLock();
	
	public ListenerTracker(BundleContext bundleContext, Engine engine, String schemaName) throws InvalidSyntaxException {
		super(
				bundleContext,
				bundleContext.createFilter(
						"(&(|"
						+ COND_LISTENER_DEPLOYMENT
						+ COND_LISTENER_TRANSACTION
						+ COND_LISTENER_PRE_MERGE
						+ COND_LISTENER_MERGE
						+ COND_LISTENER_PRE_PERSIST
						+ COND_LISTENER_PERSIST
						+ COND_LISTENER_PRE_DELETE
						+ COND_LISTENER_DELETE
						+ ")(schema=" + schemaName + "))"
				),
				null
		);
		this.engine = engine;
	}

	@Override
	public Object addingService(ServiceReference reference) {
		lock.lock();
		try {
			Object listener = super.addingService(reference);
			listeners.add(listener);
			Collection<String> schemaTypes = new DictionaryAdapter(reference).emptyStringAsNull().getStringCollection("schemaTypes");
			if (schemaTypes != null && !schemaTypes.isEmpty()) {
				String[] tpyeNamesArray = schemaTypes.toArray(new String[schemaTypes.size()]);
				engine.addListenerForTypes(listener, tpyeNamesArray);
			} else {
				engine.addListener(listener);
			}
			return listener;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		lock.lock();
		try {
			Iterator<Object> iterator = listeners.iterator();
			while (iterator.hasNext()) {
				if (iterator.next() == service) {
					iterator.remove();
				}
			}
			engine.removeListener(service);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		super.close();
		lock.lock();
		try {
			for (Object listener : listeners) {
				engine.removeListener(listener);
			}
			listeners.clear();
		} finally {
			lock.unlock();
		}
	}
	
	
}
