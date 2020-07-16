package org.bndly.schema.api.nquery;

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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ReservedKeywords {

	public static final String CMD_PICK = "PICK";
	public static final String CMD_COUNT = "COUNT";
	public static final String PARAM_WILDCARD = "?";
	public static final String NEGATION = "!";
	public static final String EQUAL = "=";
	public static final String GREATER = ">";
	public static final String GREATER_EQUAL = GREATER + EQUAL;
	public static final String LOWER = "<";
	public static final String LOWER_EQUAL = LOWER + EQUAL;
	public static final String INRANGE = "INRANGE";
	public static final String TYPED = "TYPED";
	public static final String IF_CLAUSE = "IF";
	public static final String ORDERBY = "ORDERBY";
	public static final String ORDER_DIRECTION_ASCENDING = "ASC";
	public static final String ORDER_DIRECTION_DESCENDING = "DESC";
	public static final String LIMIT = "LIMIT";
	public static final String OFFSET = "OFFSET";
	public static final String BOOLEAN_OPERATOR_AND = "AND";
	public static final String BOOLEAN_OPERATOR_OR = "OR";
	public static final String GROUP_START = "(";
	public static final String GROUP_END = ")";
	
	boolean isReservedKeyword(String keyword);
}
