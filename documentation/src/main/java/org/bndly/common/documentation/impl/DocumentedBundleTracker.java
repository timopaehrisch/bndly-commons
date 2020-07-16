package org.bndly.common.documentation.impl;

/*-
 * #%L
 * Documentation
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

import org.bndly.common.documentation.BundleDocumentation;
import org.bndly.common.documentation.BundleDocumentationProvider;
import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.RenderingListener;
import org.bndly.common.velocity.api.VelocityTemplate;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.commonmark.parser.Parser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {DocumentedBundleTracker.class, BundleDocumentationProvider.class}, immediate = true)
public class DocumentedBundleTracker implements BundleDocumentationProvider {

	static final String DOCUMENTATION_HEADER_NAME = "Bndly-Documentation";
	private static final Logger LOG = LoggerFactory.getLogger(DocumentedBundleTracker.class);

	private BundleTracker bundleTracker;
	private final Map<Long, BundleDocumentationImpl> documentationByBundleId = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Parser parser = Parser.builder().build();
	private RenderingListener renderingListener;
	
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private ContextProvider contextProvider;
	
	private ImageResource imageResource;
	
	@Reference
	private Renderer templateRenderer;
	
	/*
	Velocity Template for documentation
	#foreach ( $bundleDocumentation in ${bundleDocumentationProvider.availableBundleDocumentation})
		<h6>${bundleDocumentation.bundle.symbolicName} (${bundleDocumentation.bundle.version}) <small>${bundleDocumentation.bundle.bundleId}</small></h6>
		#foreach ( $markdownEntry in ${bundleDocumentation.markdownEntries})
			${markdownEntry.renderedMarkdown}
		#end
	#end
	*/

	@Activate
	public void activate(ComponentContext componentContext) {
		imageResource = new ImageResource(this, controllerResourceRegistry, contextProvider);
		controllerResourceRegistry.deploy(imageResource);
		final DocumentedBundleTracker that = this;
		renderingListener = new RenderingListener() {
			@Override
			public void beforeRendering(VelocityTemplate velocityTemplate, Writer writer) {
				velocityTemplate.addContextData(ContextData.newInstance("bundleDocumentationProvider", that));
			}

			@Override
			public void afterRendering(VelocityTemplate velocityTemplate, Writer writer) {
			}
		};
		bundleTracker = new BundleTracker(componentContext.getBundleContext(), Bundle.ACTIVE | Bundle.UNINSTALLED | Bundle.STOP_TRANSIENT, new BundleTrackerCustomizer() {
			@Override
			public Object addingBundle(Bundle bundle, BundleEvent be) {
				inspectBundleDocumentation(bundle);
				return bundle;
			}

			@Override
			public void modifiedBundle(Bundle bundle, BundleEvent be, Object t) {
				dropBundleDocumentation(bundle);
				inspectBundleDocumentation(bundle);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent be, Object t) {
				dropBundleDocumentation(bundle);
			}

		});
		bundleTracker.open();
		templateRenderer.addRenderingListener(renderingListener);
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		templateRenderer.removeRenderingListener(renderingListener);
		bundleTracker.close();
		controllerResourceRegistry.undeploy(imageResource);
		imageResource = null;
	}

	public BundleDocumentation getBundleDocumentationById(long bundleId) {
		return documentationByBundleId.get(bundleId);
	}
	
	@Override
	public List<BundleDocumentation> getAvailableBundleDocumentation() {
		lock.readLock().lock();
		try {
			final List<BundleDocumentationImpl> items = new ArrayList<>(documentationByBundleId.values());
			return (List) Collections.unmodifiableList(items);
		} finally {
			lock.readLock().unlock();
		}
	}

	private void inspectBundleDocumentation(Bundle bundle) {
		BundleDocumentationImpl bundleDocumentation = new BundleDocumentationImpl(bundle, imageResource);
		if (bundleDocumentation.isEmpty()) {
			return;
		}
		bundleDocumentation.indexContainedImages(parser);
		LOG.info("adding documentation of bundle {}", bundle.getSymbolicName());
		lock.writeLock().lock();
		try {
			documentationByBundleId.put(bundle.getBundleId(), bundleDocumentation);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void dropBundleDocumentation(Bundle bundle) {
		LOG.info("removing documentation of bundle {}", bundle.getSymbolicName());
		lock.writeLock().lock();
		try {
			documentationByBundleId.remove(bundle.getBundleId());
		} finally {
			lock.writeLock().unlock();
		}
	}
}
