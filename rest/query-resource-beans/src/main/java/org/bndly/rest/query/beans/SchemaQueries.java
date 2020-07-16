package org.bndly.rest.query.beans;

/*-
 * #%L
 * REST Query Resource Beans
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

/**
 * The type Schema queries.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaQueries {
	
	/**
	 * Get right value of schema query argument as object.
	 *
	 * @param argument the argument
	 * @return the object checked case by case to get right type - but only as Object return type.
	 */
	public static Object valueOf(SchemaQueryArgumentRestBean argument) {
		if (argument.getStringValue() != null) {
			return argument.getStringValue();
		} else if (argument.getBooleanValue() != null) {
			return argument.getBooleanValue();
		} else if (argument.getDateValue() != null) {
			return argument.getDateValue();
		} else if (argument.getDecimalValue() != null) {
			return argument.getDecimalValue();
		} else if (argument.getDoubleValue() != null) {
			return argument.getDoubleValue();
		} else if (argument.getLongValue() != null) {
			return argument.getLongValue();
		} else {
			return null;
		}
	}
}
