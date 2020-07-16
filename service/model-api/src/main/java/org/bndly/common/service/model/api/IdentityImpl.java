package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import org.bndly.common.reflection.ReflectionUtil;
import java.io.Serializable;
import java.lang.reflect.Field;

public class IdentityImpl implements Identity, Serializable {

	private final String sourceTypeName;
	private final String referenceFieldName;
	private final Object referenceFieldValue;

	public IdentityImpl(String sourceTypeName, String referenceFieldName, Object referenceFieldValue) {
		this.sourceTypeName = sourceTypeName;
		this.referenceFieldName = referenceFieldName;
		this.referenceFieldValue = referenceFieldValue;
	}

	@Override
	public boolean appliesTo(Object object) {
		if (object != null) {
			try {
				Class<?> sClass = getClass().getClassLoader().loadClass(sourceTypeName);
				Class<?> oClass = object.getClass();
				if (sClass.isAssignableFrom(oClass)) {
					Field field = ReflectionUtil.getFieldByName(referenceFieldName, sClass);
					if (field != null) {
						Object sV = referenceFieldValue;
						if (sV != null) {
							Object oV = ReflectionUtil.getFieldValue(field, object);
							if (oV == sV || sV.equals(oV)) {
								return true;
							}
						}

					}
				}
			} catch (ClassNotFoundException ex) {
				// i dont care about classes that do not exist anymore
				return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 19 * hash + (this.sourceTypeName != null ? this.sourceTypeName.hashCode() : 0);
		hash = 19 * hash + (this.referenceFieldName != null ? this.referenceFieldName.hashCode() : 0);
		hash = 19 * hash + (this.referenceFieldValue != null ? this.referenceFieldValue.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final IdentityImpl other = (IdentityImpl) obj;
		if ((this.sourceTypeName == null) ? (other.sourceTypeName != null) : !this.sourceTypeName.equals(other.sourceTypeName)) {
			return false;
		}
		if ((this.referenceFieldName == null) ? (other.referenceFieldName != null) : !this.referenceFieldName.equals(other.referenceFieldName)) {
			return false;
		}
		if (this.referenceFieldValue != other.referenceFieldValue && (this.referenceFieldValue == null || !this.referenceFieldValue.equals(other.referenceFieldValue))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "IdentityImpl{" + "sourceTypeName=" + sourceTypeName + ", referenceFieldName=" + referenceFieldName + ", referenceFieldValue=" + referenceFieldValue + '}';
	}

}
