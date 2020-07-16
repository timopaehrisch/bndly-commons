package org.bndly.schema.beans;

/*-
 * #%L
 * Schema Beans
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

import org.bndly.schema.api.Record;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class EclipsedSchemaBeanInvocationHandler implements InvocationHandler {

	private static class RecordInvocationHandlerBinding {

		private final Record record;
		private final SchemaBeanInvocationHandler invocationHandler;

		public RecordInvocationHandlerBinding(Record record, SchemaBeanInvocationHandler invocationHandler) {
			this.record = record;
			this.invocationHandler = invocationHandler;
		}

		public Record getRecord() {
			return record;
		}

		public SchemaBeanInvocationHandler getInvocationHandler() {
			return invocationHandler;
		}

	}
	private final RecordInvocationHandlerBinding[] bindings;
	private final boolean skipNull;

	public EclipsedSchemaBeanInvocationHandler(boolean skipNull, SchemaBeanInvocationHandler... ih) {
		this.bindings = new RecordInvocationHandlerBinding[ih.length];
		for (int i = 0; i < ih.length; i++) {
			SchemaBeanInvocationHandler handler = ih[i];
			bindings[i] = new RecordInvocationHandlerBinding(handler.getRecord(), handler);
		}
		this.skipNull = skipNull;
	}

	public EclipsedSchemaBeanInvocationHandler(boolean skipNull, SchemaBeanFactory schemaBeanFactory, Record... records) {
		this.bindings = new RecordInvocationHandlerBinding[records.length];
		for (int i = 0; i < records.length; i++) {
			Record record = records[i];
			bindings[i] = new RecordInvocationHandlerBinding(
					record, new SchemaBeanInvocationHandler(
							record, schemaBeanFactory, schemaBeanFactory.getJsonSchemaBeanFactory(), schemaBeanFactory.getEngine(), schemaBeanFactory.getInvokerMap()
					)
			);
		}
		this.skipNull = skipNull;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().startsWith("get")) {
			final String attributeName = SchemaBeanInvocationHandler.getAttributeNameFromMethod( method, "get");
			for (RecordInvocationHandlerBinding recordInvocationHandlerBinding : bindings) {
				if (recordInvocationHandlerBinding.getInvocationHandler().isAttributePresent(attributeName)) {
					Object val = recordInvocationHandlerBinding.invocationHandler.invoke(proxy, method, args);
					if (val == null && skipNull) {
						continue;
					} else {
						return val;
					}
				}
			}
			return null;
		} else if (method.getName().startsWith("set")) {
			throw new IllegalStateException("can not handle setter methods when using eclipsing.");
		}
		throw new IllegalStateException("can not handle methods that do not start with 'get' or 'set' or are not part of the ActiveRecord interface.");
	}

}
