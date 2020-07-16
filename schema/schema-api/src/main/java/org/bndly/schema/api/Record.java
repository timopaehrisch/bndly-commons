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
import org.bndly.schema.model.Type;

public interface Record {

    RecordContext getContext();
    
    void iteratePresentValues(RecordAttributeIterator listener);
    
    void iterateValues(RecordAttributeIterator listener);

    <E> E setAttributeValue(String attributeName, E value);

    <E> E getAttributeValue(String attributeName, Class<E> desiredType);

    Object getAttributeValue(String attributeName);

    Type getType();

    Long getId();

    void setId(Long id);

    void dropAttribute(String attributeName);
    
	void dropAttributes();
    
	boolean isAttributePresent(String attributeName);

    boolean isVirtualAttribute(String attributeName);

    boolean isAttributeDefined(String attributeName);
	
    Attribute getAttributeDefinition(String attributeName);

    <E extends Attribute> E getAttributeDefinition(String attributeName, Class<E> definitionType);

    boolean isReference();

    void setIsReference(boolean isReference);
	
	boolean isDirty();
}
