package org.bndly.pdf.style;

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

import org.bndly.pdf.PrintingContext;
import java.util.ArrayList;
import java.util.List;

import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.PrintingObjectImpl;

public class Style extends PrintingObjectImpl {
	
	private List<StyleAttribute<?>> attributes;

	public Style(PrintingContext context, PrintingObject owner) {
		super(context, owner);
	}

	public StringStyleAttribute createStringAttribute() {
		return addAttribute(new StringStyleAttribute(getContext(), this));
	}
	public NumericStyleAttribute createNumericAttribute() {
		return addAttribute(new NumericStyleAttribute(getContext(), this));
	}
	public EnumStyleAttribute createEnumAttribute() {
		return addAttribute(new EnumStyleAttribute(getContext(), this));
	}
	
	private <T extends StyleAttribute> T addAttribute(T attribute) {
		if (attributes == null) {
			attributes = new ArrayList<>();
		}
		attributes.add(attribute);
		return attribute;
	}

	public void set(String attributeName, Object attributeValue) {
		if (attributeValue == null) {
			throw new NullPointerException("attribute values can not be null");
		}
		if (attributeName != null) {
			StyleAttribute<?> attribute = null;
			if (attributes != null) {
				for (StyleAttribute<?> a : attributes) {
					String name = a.getName();
					if (name.equals(attributeName)) {
						attribute = a;
						break;
					}
				}
			}
			if (attribute == null) {
				Class<? extends Object> valueClass = attributeValue.getClass();
				if (valueClass.equals(String.class)) {
					attribute = createStringAttribute();
				} else if (valueClass.equals(Double.class)) {
					attribute = createNumericAttribute();
				} else if (Enum.class.isAssignableFrom(valueClass)) {
					attribute = createEnumAttribute();
				} else {
					throw new IllegalStateException("unsupported attribute value type");
				}
				attribute.setName(attributeName);
			}
			((StyleAttribute<Object>) attribute).setValue(attributeValue);
		}
	}
	
	public <T> T get(String attributeName) {
		if (attributeName != null && attributes != null) {
			for (StyleAttribute<? extends Object> attribute : attributes) {
				if (attributeName.equals(attribute.getName())) {
					return (T) attribute.getValue();
				}
			}
		}
		return null;
	}

	public boolean contains(String attributeName) {
		if (attributes != null) {
			for (StyleAttribute<? extends Object> attribute : attributes) {
				if (attributeName.equals(attribute.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public List<StyleAttribute<?>> getAllAttributes() {
		if (attributes != null) {
			return new ArrayList<>(attributes);
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (attributes != null) {
			boolean first = true;
			for (StyleAttribute att : attributes) {
				if (!first) {
					sb.append("\n");
				}
				sb.append(att.getName());
				sb.append(": ");
				sb.append(att.getValue());
				first = false;
			}
		}
		return sb.toString();
	}

	public void overwriteTo(Style overflowToStyle) {
		if (attributes != null) {
			for (StyleAttribute attribute : attributes) {
				String name = attribute.getName();
				Object value = attribute.getValue();
				overflowToStyle.set(name, value);
			}
		}
	}
	
	public boolean isLeftAligned() {
		String voAlign = get(StyleAttributes.HORIZONTAL_ALIGN);
		return ("left".equalsIgnoreCase(voAlign));
	}
	
	public boolean isRightAligned() {
		String voAlign = get(StyleAttributes.HORIZONTAL_ALIGN);
		return ("right".equalsIgnoreCase(voAlign));
	}
}
