package org.bndly.rest.atomlink.impl;

/*-
 * #%L
 * REST Link Injector
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

import org.bndly.common.lang.TransformingIterator;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.DelegatingLinkFactory;
import org.bndly.rest.atomlink.api.LinkFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class DelegatingLinkFactroyImpl implements DelegatingLinkFactory {

	private final List<LinkFactory> delegates = new ArrayList<>();
	private final Class<?> targetType;

	public DelegatingLinkFactroyImpl(Class<?> targetType) {
		this.targetType = targetType;
		if (targetType == null) {
			throw new IllegalArgumentException("targetType is not allowed to be null");
		}
	}

	@Override
	public Class getTargetType() {
		return targetType;
	}

	@Override
	public boolean isSupportingSubTypes() {
		for (LinkFactory delegate : delegates) {
			if (delegate.isSupportingSubTypes()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void registerLinkFactory(LinkFactory delegate) {
		if (delegate != null) {
			delegates.add(delegate);
		}
	}

	@Override
	public void unregisterLinkFactory(LinkFactory delegate) {
		if (delegate != null) {
			Iterator<LinkFactory> it = delegates.iterator();
			while (it.hasNext()) {
				LinkFactory item = it.next();
				if (item == delegate) {
					it.remove();
				}
			}
		}
	}

	@Override
	public Iterator<AtomLinkBean> buildLinks(final Object targetBean, final boolean isMessageRoot) {
		Class<?> aClass = targetBean.getClass();
		final Iterator<LinkFactory> iter = new TransformingIterator<LinkFactory, LinkFactory>(delegates.iterator()) {

			@Override
			protected LinkFactory transform(LinkFactory toTransform) {
				return toTransform;
			}

			@Override
			protected boolean isAccepted(LinkFactory toCheck) {
				if (toCheck.isSupportingSubTypes()) {
					return true;
				} else {
					return toCheck.getTargetType().equals(aClass);
				}
			}
		};
		return new Iterator<AtomLinkBean>() {

			private LinkFactory currentFactroy;
			private Iterator<AtomLinkBean> currentIter;
			
			@Override
			public boolean hasNext() {
				if (currentFactroy == null) {
					boolean hasNextFactory = iter.hasNext();
					if (!hasNextFactory) {
						return false;
					} else {
						currentFactroy = iter.next();
						currentIter = currentFactroy.buildLinks(targetBean, isMessageRoot);
					}
				}
				if (currentIter.hasNext()) {
					return true;
				} else {
					currentFactroy = null;
					currentIter = null;
					return hasNext();
				}
			}

			@Override
			public AtomLinkBean next() {
				if (currentIter == null) {
					throw new IllegalStateException("do not invoke iter.next() without calling hasNext before.");
				}
				return currentIter.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("this iterator does not support iter.remove()");
			}
		};
	}

	@Override
	public Iterator buildLinks() {
		return buildLinks(null, true);
	}

}
