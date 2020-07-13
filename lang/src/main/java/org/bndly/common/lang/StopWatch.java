package org.bndly.common.lang;

/*-
 * #%L
 * Lang
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
public final class StopWatch {

	private long s = -1;
	private long e = -1;

	public final StopWatch start() {
		s = System.currentTimeMillis();
		return this;
	}

	public final StopWatch stop() {
		e = System.currentTimeMillis();
		return this;
	}
	
	public final StopWatch reset() {
		s = -1;
		e = -1;
		return this;
	}

	public final long getMillis() {
		return e - s;
	}
	
	public final long getMillisNow() {
		return System.currentTimeMillis() - s;
	}
}
