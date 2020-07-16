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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PlexusLoggerToMavenLoggerAdapter implements Log {
	private final Logger logger;

	public PlexusLoggerToMavenLoggerAdapter(Logger logger) {
		this.logger = logger;
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void debug(CharSequence cs) {
		logger.debug(cs.toString());
	}

	@Override
	public void debug(CharSequence cs, Throwable thrwbl) {
		logger.debug(cs.toString(), thrwbl);
	}

	@Override
	public void debug(Throwable thrwbl) {
		logger.debug("", thrwbl);
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public void info(CharSequence cs) {
		logger.info(cs.toString());
	}

	@Override
	public void info(CharSequence cs, Throwable thrwbl) {
		logger.info(cs.toString(), thrwbl);
	}

	@Override
	public void info(Throwable thrwbl) {
		logger.info("", thrwbl);
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public void warn(CharSequence cs) {
		logger.warn(cs.toString());
	}

	@Override
	public void warn(CharSequence cs, Throwable thrwbl) {
		logger.warn(cs.toString(), thrwbl);
	}

	@Override
	public void warn(Throwable thrwbl) {
		logger.warn("", thrwbl);
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public void error(CharSequence cs) {
		logger.error(cs.toString());
	}

	@Override
	public void error(CharSequence cs, Throwable thrwbl) {
		logger.error(cs.toString(), thrwbl);
	}

	@Override
	public void error(Throwable thrwbl) {
		logger.error("", thrwbl);
	}
	
}
