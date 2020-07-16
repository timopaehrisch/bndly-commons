package org.bndly.common.datasource;

/*-
 * #%L
 * Data Source
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

import org.bndly.common.data.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DriverBundleListener implements BundleListener {

	@Override
	public void bundleChanged(BundleEvent be) {
		Bundle bundle = be.getBundle();
		int eventType = be.getType();
		if (eventType == BundleEvent.INSTALLED) {
			onBundleInstalled(bundle);
		} else if (eventType == BundleEvent.RESOLVED) {
			onBundleResolved(bundle);
		} else if (eventType == BundleEvent.STARTED) {
			onBundleStarted(bundle);
		} else if (eventType == BundleEvent.STARTING) {
			onBundleStarting(bundle);
		} else if (eventType == BundleEvent.STOPPED) {
			onBundleStopped(bundle);
		} else if (eventType == BundleEvent.STOPPING) {
			onBundleStopping(bundle);
		} else if (eventType == BundleEvent.UNINSTALLED) {
			onBundleUninstalled(bundle);
		} else if (eventType == BundleEvent.UNRESOLVED) {
			onBundleUnresolved(bundle);
		} else if (eventType == BundleEvent.UPDATED) {
			onBundleUpdated(bundle);
		}
	}

	protected final List<String> getSqlDriverNamesFromBundle(Bundle bundle) throws IOException {
		URL url = bundle.getResource("META-INF/services/java.sql.Driver");
		if (url == null) {
			return null;
		}
		InputStream stream = url.openStream();
		if (stream == null) {
			return null;
		}
		String driverClassName = IOUtils.readToString(stream, "UTF-8");
		if (driverClassName == null) {
			return null;
		}
		driverClassName = driverClassName.trim();
		List<String> res = new ArrayList<>();
		String[] classNames = driverClassName.split("\n");
		for (int i = 0; i < classNames.length; i++) {
			String className = classNames[i];
			className = className.trim();
			if (!className.isEmpty() && !className.startsWith("#")) {
				res.add(className);
			}
		}

		return res;
	}

	protected void onBundleInstalled(Bundle bundle) {}
	protected void onBundleResolved(Bundle bundle) {}
	protected void onBundleStarted(Bundle bundle) {}
	protected void onBundleStarting(Bundle bundle) {}
	protected void onBundleStopped(Bundle bundle) {}
	protected void onBundleStopping(Bundle bundle) {}
	protected void onBundleUninstalled(Bundle bundle) {}
	protected void onBundleUnresolved(Bundle bundle) {}
	protected void onBundleUpdated(Bundle bundle) {}
}
