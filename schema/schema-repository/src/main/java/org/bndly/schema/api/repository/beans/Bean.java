package org.bndly.schema.api.repository.beans;

/*-
 * #%L
 * Schema Repository
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

import org.bndly.schema.api.repository.Morphable;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Bean extends Morphable {
	BeanResolver getBeanResolver();
	String getName();
	String getPath();
	String getBeanType();
	Bean getParent();
	Bean getChild(String name);
	Iterator<Bean> getChildren();
	Map<String, Object> getProperties();
	Object getProperty(String name);
	/**
	 * This is a Java Bean EL convenience getter, for fast access of properties. It is identical to {@link #getProperty(java.lang.String)}.
	 * @param name The name of the property to retrieve.
	 */
	Object get(String name);
}
