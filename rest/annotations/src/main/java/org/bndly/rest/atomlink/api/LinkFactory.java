package org.bndly.rest.atomlink.api;

/*-
 * #%L
 * REST Atom Link API
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

import java.util.Iterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface LinkFactory<E> {
	Class<E> getTargetType();

	/**
	 * This method indicates if the created links shall be injected also in subclasses of the {@link #getTargetType()}.
	 * @return
	 */
	default boolean isSupportingSubTypes() {
		return false;
	}

	Iterator<AtomLinkBean> buildLinks(E targetBean, boolean isMessageRoot);
	/**
	 * This is a convenience method for {@link #buildLinks(java.lang.Object, boolean) } with the parameters <code>buildLinks(null, true)</code>.
	 * @return an iterator over created atom link beans
	 */
	Iterator<AtomLinkBean> buildLinks();
}
