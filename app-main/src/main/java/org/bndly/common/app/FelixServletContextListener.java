package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleContext;

@WebListener
public class FelixServletContextListener implements ServletContextListener {

	private FelixMain felixMain;
	private ServletContext sc;
	private final FelixMainListener listener = new FelixMainListener() {
		@Override
		public void beforeStart(Felix felix) {
			BundleContext bundleContext = felix.adapt(BundleContext.class);
			sc.setAttribute(BundleContext.class.getName(), bundleContext);
		}

		@Override
		public void beforeStop(Felix felix) {
			sc.removeAttribute(BundleContext.class.getName());
		}

	};

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		sc = sce.getServletContext();
		final Map<String, Object> mainConfig = new HashMap<>();
		felixMain = new FelixMain(new WebappEnvironment(), new Logger() {
			@Override
			public void info(String message) {
				sc.log(message);
			}

			@Override
			public void debug(String message) {
				sc.log(message);
			}

			@Override
			public void warn(String message) {
				sc.log(message);
			}

			@Override
			public void error(String message) {
				sc.log(message);
			}
		}, mainConfig).addListener(listener);
		try {
			felixMain.start();
		} catch (Exception ex) {
			throw new IllegalStateException("could not start felix", ex);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			felixMain.stop();
			felixMain.removeListener(listener);
			felixMain.destroy();
		} catch (Exception ex) {
			throw new IllegalStateException("could not stop felix", ex);
		}
	}
	
}
