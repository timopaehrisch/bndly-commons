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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class Java11JaxbSupport {

	private static final ThreadLocal<Boolean> IN_LOOKUP = ThreadLocal.withInitial(new Supplier<Boolean>() {
		@Override
		public Boolean get() {
			return Boolean.FALSE;
		}
	});
	private static final String CONFIG_PROP_JAXB_BUNDLE = SharedConstants.SYSTEM_PROPERTY_JAXB_IMPL_BUNDLE;
	private static final String JAXB_IMPL_BUNDLE_SYMBOLIC_NAME = "com.sun.xml.bind.jaxb-impl";

	private static ClassLoader concatClassLoaders(ClassLoader rootClassLoader, Iterable<Bundle> bundles) {
		Iterator<Bundle> iterator = bundles.iterator();
		if (!iterator.hasNext()) {
			throw new IllegalArgumentException("provide at least one bundle");
		}
		StringBuilder sb = null;
		for (Bundle bundle : bundles) {
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append(",");
			}
			sb.append(bundle.getSymbolicName());
		}
		final String classloadersString = sb.toString();
		return new ClassLoader(rootClassLoader) {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				for (Bundle bundle : bundles) {
					ClassLoader classLoader = bundle.adapt(BundleWiring.class).getClassLoader();
					try {
						return classLoader.loadClass(name);
					} catch (ClassNotFoundException e) {
						// skip
					}
				}
				throw new ClassNotFoundException("could not find class " + name + " in " + classloadersString);
			}

			@Override
			protected URL findResource(String name) {
				for (Bundle bundle : bundles) {
					URL resource = bundle.getResource(name);
					if (resource != null) {
						return resource;
					}
				}
				return null;
			}

			@Override
			protected Enumeration<URL> findResources(String name) throws IOException {
				Set<URL> urls = new LinkedHashSet<>();
				for (Bundle bundle : bundles) {
					Enumeration<URL> resources = bundle.getResources(name);
					while (resources.hasMoreElements()) {
						URL url = resources.nextElement();
						if (!urls.contains(url)) {
							urls.add(url);
						}
					}
				}
				return Collections.enumeration(urls);
			}

		};
	}

	/**
	 *
	 * @param contextPath {@code :} separated java packages to scan for jaxb classes
	 * @param classLoader
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	public static Object createContext(String contextPath, ClassLoader classLoader, Map<String, Object> properties) throws Exception {
		Felix currentFelix = Java11Support.getCurrentFelix();
		if (currentFelix == null) {
			throw new IllegalStateException("no Felix instance available");
		} else {
			if (IN_LOOKUP.get()) {
				throw new IllegalStateException("recursive creation of JAXBContext");
			}
			IN_LOOKUP.set(Boolean.TRUE);
			Map currentFelixConfig = Java11Support.getCurrentFelixConfig();
			String symbolicNameOfJaxbImplBundle = (String) currentFelixConfig.get(CONFIG_PROP_JAXB_BUNDLE);
			if (symbolicNameOfJaxbImplBundle == null || symbolicNameOfJaxbImplBundle.isEmpty()) {
				symbolicNameOfJaxbImplBundle = JAXB_IMPL_BUNDLE_SYMBOLIC_NAME;
			}
			try {
				// we now mess with the package visibility in OSGI...
				// ... we will index all bundles by their exported packages.
				// then we know which bundle classloaders we have to concatenate for looking up jaxb classes from the passed in packages
				String[] packageNames = contextPath.split(":");
				final Map<String, Bundle> bundlesByPackage = new HashMap<>();
				Bundle jaxbImplBundle = null;
				Bundle[] bundles = currentFelix.getBundleContext().getBundles();
				for (final Bundle bundle : bundles) {
					if (symbolicNameOfJaxbImplBundle.equals(bundle.getSymbolicName())) {
						jaxbImplBundle = bundle;
					}
					int state = bundle.getState();
					if (state == Bundle.ACTIVE || state == Bundle.STARTING) {
						String exportPackageHeader = bundle.getHeaders().get("Export-Package");
						parsePackagesFromHeader(exportPackageHeader, new Consumer<String>() {
							@Override
							public void accept(String packageName) {
								bundlesByPackage.put(packageName, bundle);
							}
						});
					}
				}
				
				Set<Bundle> bundlesToScanForPackage = new HashSet<>();
				for(String packageName : packageNames) {
					Bundle bundle = bundlesByPackage.get(packageName);
					if(bundle == null) {
						throw new IllegalStateException("could not find active bundle for package "+packageName);
					}
					bundlesToScanForPackage.add(bundle);
				}
				
				ClassLoader classLoaderToUse = jaxbImplBundle == null ? classLoader : jaxbImplBundle.adapt(BundleWiring.class).getClassLoader();
				if (!bundlesToScanForPackage.isEmpty()) {
					List<ClassLoader> classLoaders = new ArrayList<>(bundlesToScanForPackage.size());
					for (Bundle bundle : bundlesToScanForPackage) {
						classLoaders.add(bundle.adapt(BundleWiring.class).getClassLoader());
					}

					classLoaderToUse = concatClassLoaders(classLoaderToUse, bundlesToScanForPackage);
				}
				
				return createInstanceOfJaxbContextWithBundleAsContext(jaxbImplBundle == null ? currentFelix.getBundle() : jaxbImplBundle, contextPath, classLoaderToUse, properties);
			} finally {
				IN_LOOKUP.set(Boolean.FALSE);
			}
		}
	}

	public static Object createContext(Class[] classes, Map properties) throws Exception {
		Felix currentFelix = Java11Support.getCurrentFelix();
		if (currentFelix == null) {
			throw new IllegalStateException("no Felix instance available");
		} else {
			if (IN_LOOKUP.get()) {
				throw new IllegalStateException("recursive creation of JAXBContext");
			}
			IN_LOOKUP.set(Boolean.TRUE);
			Map currentFelixConfig = Java11Support.getCurrentFelixConfig();
			String symbolicNameOfJaxbImplBundle = (String) currentFelixConfig.get(CONFIG_PROP_JAXB_BUNDLE);
			if (symbolicNameOfJaxbImplBundle == null || symbolicNameOfJaxbImplBundle.isEmpty()) {
				symbolicNameOfJaxbImplBundle = JAXB_IMPL_BUNDLE_SYMBOLIC_NAME;
			}
			try {
				Bundle[] bundles = currentFelix.getBundleContext().getBundles();
				for (Bundle bundle : bundles) {
					if (symbolicNameOfJaxbImplBundle.equals(bundle.getSymbolicName())) {
						return createInstanceOfJaxbContextWithBundleAsContext(bundle, classes, properties);
					}
				}
				// fall back to system bundle and assume, the the outer class loaders provide the jaxb implementation (could be a Java 8 JRE or a WEB-INF/lib in Java 11

				// please note: if the jaxb api is provided as a bundle, then the java8 JRE may not provide a compatible implementation
				return createInstanceOfJaxbContextWithBundleAsContext(currentFelix.getBundle(), classes, properties);
			} finally {
				IN_LOOKUP.set(Boolean.FALSE);
			}
		}
	}

	private static <E> E doInContextOfBundleClassloader(Bundle bundle, Callable<E> callable) throws Exception {
		Thread currentThread = Thread.currentThread();
		ClassLoader contextClassLoader = currentThread.getContextClassLoader();
		ClassLoader bundlesClassloader = bundle.adapt(BundleWiring.class).getClassLoader();
		currentThread.setContextClassLoader(bundlesClassloader);
		try {
			return callable.call();
		} finally {
			currentThread.setContextClassLoader(contextClassLoader);
		}
	}

	private static Object createInstanceOfJaxbContextWithBundleAsContext(Bundle bundle, Class[] classes, Map properties) throws Exception {
		return doInContextOfBundleClassloader(bundle, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
				try {
					// since java 9
					Class<?> jaxContextFactoryClass = contextClassLoader.loadClass("javax.xml.bind.JAXBContextFactory");
					ServiceLoader<?> factories = ServiceLoader.load(jaxContextFactoryClass, contextClassLoader);
					Iterator<?> iter = factories.iterator();
					while (iter.hasNext()) {
						Object ctxFactory = iter.next();
						Method newInstanceMethod = ctxFactory.getClass().getMethod("createContext", Class[].class, Map.class);
						return newInstanceMethod.invoke(ctxFactory, classes, properties);
					}
				} catch (ClassNotFoundException e) {
					// seems like we are running in java 8
					// 
				}
				Class<?> jaxbContextClass = contextClassLoader.loadClass("javax.xml.bind.JAXBContext");
				Method newInstanceMethod = jaxbContextClass.getMethod("newInstance", Class[].class, Map.class);
				return newInstanceMethod.invoke(null, classes, properties);
			}
		});
	}

	private static Object createInstanceOfJaxbContextWithBundleAsContext(Bundle bundle, String contextPath, ClassLoader classLoader, Map properties) throws Exception {
		return doInContextOfBundleClassloader(bundle, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
				try {
					// since java 9
					Class<?> jaxContextFactoryClass = contextClassLoader.loadClass("javax.xml.bind.JAXBContextFactory");
					ServiceLoader<?> factories = ServiceLoader.load(jaxContextFactoryClass, contextClassLoader);
					Iterator<?> iter = factories.iterator();
					while (iter.hasNext()) {
						Object ctxFactory = iter.next();
						Method newInstanceMethod = ctxFactory.getClass().getMethod("createContext", String.class, ClassLoader.class, Map.class);
						return newInstanceMethod.invoke(ctxFactory, contextPath, classLoader, properties);
					}
				} catch (ClassNotFoundException e) {
					// seems like we are running in java 8
					// 
				}
				Class<?> jaxbContextClass = contextClassLoader.loadClass("javax.xml.bind.JAXBContext");
				Method newInstanceMethod = jaxbContextClass.getMethod("newInstance", String.class, ClassLoader.class, Map.class);
				return newInstanceMethod.invoke(null, contextPath, classLoader, properties);
			}
		});
	}

	private static void parsePackagesFromHeader(String header, Consumer<String> consumer) {
		new PackageHeaderParser().parse(header, new Consumer<PackageHeaderParser.PackageDescription>() {
			@Override
			public void accept(PackageHeaderParser.PackageDescription packageDescription) {
				consumer.accept(packageDescription.getName());
			}
		});
	}
}
