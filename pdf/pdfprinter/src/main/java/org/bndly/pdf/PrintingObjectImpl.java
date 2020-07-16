package org.bndly.pdf;

/*-
 * #%L
 * PDF Document Printer
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PrintingObjectImpl implements PrintingObject {
	private static final Logger LOG = LoggerFactory.getLogger(PrintingObject.class);
	
	private final long id;
	private String itemId;
	private final PrintingObject owner;
	private final PrintingContext context;
	
	protected PrintingObjectImpl(PrintingContext context, PrintingObject owner) {
		if (context == null) {
			throw new IllegalArgumentException("context is not allowed to be empty");
		}
		this.context = context;
		if (owner == null) {
			throw new IllegalArgumentException("owner is not allowed to be empty");
		}
		this.owner = owner;
		this.id = context.plusplus();
	}

	@Override
	public final PrintingContext getContext() {
		return context;
	}
//	public long getId() {
//		return id;
//	}
//	public <T extends PrintingObject> T create(Class<T> type) {
//		T instance = null;
//		try {
//			instance = type.newInstance();
//		} catch (InstantiationException | IllegalAccessException e) {
//			LOG.error("failed to create instance of " + type + " in a printing object", e);
//		}
//		if (instance == null) {
//			throw new IllegalStateException("could not instantiate " + type.getSimpleName());
//		}
//		((PrintingObject) instance).owner = this;
//		if (context != null) {
//			((PrintingObject) instance).context = context;
//		} else {
//			((PrintingObject) instance).context = getContext();
//		}
//		instance.init();
//		return instance;
//	}

	@Override
	public final long getId() {
		return id;
	}
	
	@Override
	public final String getItemId() {
		return itemId;
	}
	
	public final void setItemId(String itemId) {
		this.itemId = itemId;
	}
	
	public final PrintingObject getOwner() {
		return owner;
	};
	
	@Override
	public final boolean is(Class<? extends PrintingObject> clazz) {
		return clazz.isAssignableFrom(getClass());
	}
	
	@Override
	public final <T extends PrintingObject> T as(Class<T> type) {
		return type.cast(this);
	}
	
}
