package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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

import org.bndly.common.bpm.api.OSGIConstants;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JavaDelegateWrapper implements JavaDelegate {

	private final JavaDelegate wrapped;
	private final DictionaryAdapter dictionaryAdapter;

	public JavaDelegateWrapper(JavaDelegate wrapped, DictionaryAdapter dictionaryAdapter) {
		if (wrapped == null) {
			throw new IllegalArgumentException("wrapped is not allowed to be null");
		}
		this.wrapped = wrapped;
		if (dictionaryAdapter == null) {
			throw new IllegalArgumentException("dictionaryAdapter is not allowed to be null");
		}
		this.dictionaryAdapter = dictionaryAdapter;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		wrapped.execute(execution);
	}

	public JavaDelegate getWrapped() {
		return wrapped;
	}
	
	public boolean isGlobal() {
		return dictionaryAdapter.getBoolean(OSGIConstants.IS_GLOBAL_DELEGATE, Boolean.FALSE);
	}

	public String getTargetEngine() {
		return dictionaryAdapter.getString(OSGIConstants.TARGET_ENGINE);
	}

	public String getName() {
		return dictionaryAdapter.getString(OSGIConstants.DELEGATE_NAME);
	}

}
