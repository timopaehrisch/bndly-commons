package org.bndly.common.json.api;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.model.JSValue;
import java.lang.reflect.Type;

public interface KeyConverter {
//    Object getKeyObjectFromJSONElementNameForJavaType(String jsonKey, Class<KEYED_OBJECT> object);
//    /**
//     * the returned string will be used as the node name in the resulting JSON object
//     * @param object the object that shall be keyed
//     * @return the string that will be used as a JSON node name for the provided map entry
//     */
//    String getKeyForJSONElementForJavaObject(KEYED_OBJECT object);
	
	boolean canCreateJSONKey(Type mapType, Type entryType, ConversionContext conversionContext, Object javaKey, Object javaValue);

	String createJSONKey(Type mapType, Type entryType, ConversionContext conversionContext, Object javaKey, Object javaValue);
	
	boolean canCreateJavaKey(Type mapType, Type keyType, Type entryType, ConversionContext conversionContext, JSValue jsValue, String jsKey, Object deserializedJavaValue);
	
	Object createJavaKey(Type mapType, Type keyType, Type entryType, ConversionContext conversionContext, JSValue jsValue, String jsKey, Object deserializedJavaValue);
}
