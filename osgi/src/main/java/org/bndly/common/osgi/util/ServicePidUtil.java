package org.bndly.common.osgi.util;

/*-
 * #%L
 * OSGI Utilities
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
public final class ServicePidUtil {

	private ServicePidUtil() {
	}
	
	public static interface PID {
		String getPid();
		String getFactoryPid();
	}
	
	public static PID parseFileName(String pidFileName) {
		// strip extension
		int endOfPid = pidFileName.lastIndexOf('.');
		String pidTmp = pidFileName.substring(0, endOfPid);
		return parsePid(pidTmp);
	}
	
	public static PID parsePid(String pidWithoutFileExtension) {
		// look for factory pid
		int n = pidWithoutFileExtension.indexOf('-');
		final String factoryPid;
		final String pid;
		if (n > 0) {
			factoryPid = pidWithoutFileExtension.substring(n + 1);
			pid = pidWithoutFileExtension.substring(0, n);
		} else {
			factoryPid = null;
			pid = pidWithoutFileExtension;
		}
		return new PID() {
			@Override
			public String getPid() {
				return pid;
			}

			@Override
			public String getFactoryPid() {
				return factoryPid;
			}

			@Override
			public String toString() {
				return "PID: " + getPid() + " FactoryPID: " + getFactoryPid();
			}

		};
	}
}
