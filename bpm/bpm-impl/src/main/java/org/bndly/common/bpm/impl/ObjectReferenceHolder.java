package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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

public final class ObjectReferenceHolder<E> {
	private E ref;
	
	public ObjectReferenceHolder() {
	}

	public ObjectReferenceHolder(E obj) {
		ref = obj;
	}
	
	public void setRef(E ref) {
		this.ref = ref;
	}
	public E getRef() {
		return ref;
	}
	public boolean isNull() {
		return ref == null;
	}
}
