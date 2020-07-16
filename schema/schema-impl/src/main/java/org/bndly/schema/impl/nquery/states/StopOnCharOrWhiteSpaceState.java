package org.bndly.schema.impl.nquery.states;

/*-
 * #%L
 * Schema Impl
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
public abstract class StopOnCharOrWhiteSpaceState extends TokenState {

	private final String stopChars;

	public StopOnCharOrWhiteSpaceState(String stopChars) {
		this.stopChars = stopChars;
	}
	
	@Override
	protected boolean isStopChar(char character) {
		return stopChars.indexOf(character) > -1 || super.isStopChar(character);
	}

	
}
