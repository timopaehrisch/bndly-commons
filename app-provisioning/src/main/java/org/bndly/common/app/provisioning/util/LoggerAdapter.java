package org.bndly.common.app.provisioning.util;

/*-
 * #%L
 * App Provisioning
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

import org.bndly.common.app.Logger;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class LoggerAdapter implements Logger {

	private final Log log;

	public LoggerAdapter(Log log) {
		this.log = log;
	}

	@Override
	public void info(String message) {
		log.info(message);
	}

	@Override
	public void debug(String message) {
		log.debug(message);
	}

	@Override
	public void warn(String message) {
		log.warn(message);
	}

	@Override
	public void error(String message) {
		log.error(message);
	}
	
}
