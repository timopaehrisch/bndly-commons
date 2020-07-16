package org.bndly.schema.api;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.SimpleAttribute;
import org.bndly.schema.model.Type;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class NQueryUtil {

	private NQueryUtil() {
	}
	
	public static interface NQuery {
		String getQueryString();
		Object[] getQueryArgs();
	}
	
	public static NQuery queryByExampleFromRecordWithoutNullAttributes(Record record) {
		if (record == null) {
			return null;
		}
		Type type = record.getType();
		ArrayList<Object> args = new ArrayList<>();
		StringBuffer sb = new StringBuffer();
		String firstChar = type.getName().substring(0, 1).toLowerCase();
		sb.append("PICK ").append(type.getName()).append(" ").append(firstChar);
		StringBuffer ifStringBuffer = new StringBuffer();

		appendAttributesOfRecord(record, ifStringBuffer, args, firstChar);

		if (!args.isEmpty()) {
			sb.append(" IF ").append(ifStringBuffer.substring(5));
		}

		final String string = sb.toString();
		final Object[] argsArray = args.toArray();
		return new NQuery() {

			@Override
			public String getQueryString() {
				return string;
			}

			@Override
			public Object[] getQueryArgs() {
				return argsArray;
			}
		};
	}
	
	private static void appendAttributesOfRecord(Record record, final StringBuffer ifStringBuffer, final List<Object> args, final String attributePrefix) {
		record.iteratePresentValues(new RecordAttributeIterator() {

			@Override
			public void handleAttribute(Attribute attribute, Record record) {
				if (attribute.isVirtual()) {
					return;
				}
				if (BinaryAttribute.class.isInstance(attribute) || InverseAttribute.class.isInstance(attribute)) {
					return;
				}
				Object raw = record.getAttributeValue(attribute.getName());
				if (raw == null) {
					return;
				}
				String prefix = attributePrefix + "." + attribute.getName();
				if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					if (Record.class.isInstance(raw)) {
						Record rawRec = (Record) raw;
						// append type info to query
						ifStringBuffer.append(" AND ").append(prefix).append(" TYPED ?");
						args.add(rawRec.getType().getName());

						appendAttributesOfRecord(rawRec, ifStringBuffer, args, prefix);
					} else if (Long.class.isInstance(raw)) {
						ifStringBuffer.append(prefix).append("=?");
						args.add(raw);
					}
				} else if (SimpleAttribute.class.isInstance(attribute)) {
					ifStringBuffer.append(" AND ").append(prefix).append("=?");
					args.add(raw);
				}
			}
		});
	}
	
}
