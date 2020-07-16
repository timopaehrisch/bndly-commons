package org.bndly.search.schema.impl;

/*-
 * #%L
 * Search Schema
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
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import org.bndly.search.api.DocumentFieldValue;
import java.util.ArrayList;
import java.util.List;

public class DefaultRecordSearchDocumentAdapter implements RecordSearchDocumentAdapter {

	private final Type type;
	private final List<HandledAttribute> handledAttributes;

	private static class HandledAttribute {

		private final NamedAttributeHolder declaredBy;
		private final Attribute attribute;

		public HandledAttribute(NamedAttributeHolder declaredBy, Attribute attribute) {
			this.declaredBy = declaredBy;
			this.attribute = attribute;
		}

		public NamedAttributeHolder getDeclaredBy() {
			return declaredBy;
		}

		public Attribute getAttribute() {
			return attribute;
		}
	}

	public DefaultRecordSearchDocumentAdapter(Type type) {
		if (type == null) {
			throw new IllegalArgumentException("can not create record search adapter without type object");
		}
		this.type = type;
		handledAttributes = new ArrayList<>();
		findHandledAttributes(handledAttributes, type);
	}

	@Override
	public Type getType() {
		return type;
	}

	private void findHandledAttributes(List<HandledAttribute> handledAttributes, NamedAttributeHolder nah) {
		if (nah == null) {
			return;
		}
		List<Attribute> a = nah.getAttributes();
		if (a != null) {
			for (Attribute attribute : a) {
				if (InverseAttribute.class.isInstance(attribute)) {
					continue;
				} else if (BinaryAttribute.class.isInstance(attribute)) {
					continue;
				} else {
					handledAttributes.add(new HandledAttribute(nah, attribute));
				}
			}
		}
		if (Type.class.isInstance(nah)) {
			Type t = Type.class.cast(nah);
			findHandledAttributes(handledAttributes, t.getSuperType());
			List<Mixin> m = t.getMixins();
			if (m != null) {
				for (Mixin mixin : m) {
					findHandledAttributes(handledAttributes, mixin);
				}
			}
		}
	}

	@Override
	public List<DocumentFieldValue> buildDocumentFieldValues(final Record record) {
		if (record.getType() != type) {
			throw new IllegalArgumentException("provided record is of wrong type.");
		}

		List<DocumentFieldValue> l = new ArrayList<>();
		for (HandledAttribute attribute : handledAttributes) {
			DocumentFieldValue v = buildFieldValue(record, attribute);
			if (v != null) {
				l.add(v);
			}
		}
		l.add(new RecordIdFieldValue(record));
		l.add(new DocumentFieldValue() {

			@Override
			public String getFieldName() {
				return "_type";
			}

			@Override
			public Object getValue() {
				return record.getType().getName();
			}
		});
		return l;
	}

	private DocumentFieldValue buildFieldValue(final Record record, final HandledAttribute handledAttribute) {
		DocumentFieldValue documentValue = new DocumentFieldValue() {
			@Override
			public String getFieldName() {
				return handledAttribute.getDeclaredBy().getName() + "_" + handledAttribute.getAttribute().getName();
			}

			@Override
			public Object getValue() {
				if (NamedAttributeHolderAttribute.class.isInstance(handledAttribute.getAttribute())) {
					// this will return the id instead of a record
					Long v = record.getAttributeValue(handledAttribute.getAttribute().getName(), Long.class);
					return v;
				} else {
					Object v = record.getAttributeValue(handledAttribute.getAttribute().getName());
					return v;
				}
			}
		};
		return documentValue;
	}
}
