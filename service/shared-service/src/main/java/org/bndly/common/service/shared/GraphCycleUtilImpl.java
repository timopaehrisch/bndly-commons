package org.bndly.common.service.shared;

/*-
 * #%L
 * Service Shared Impl
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

import org.bndly.common.graph.BeanGraphIteratorListener;
import org.bndly.common.graph.CompiledBeanIterator;
import org.bndly.common.service.shared.api.GraphCycleUtil;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class GraphCycleUtilImpl implements GraphCycleUtil {
	
	private BeanGraphIteratorListener acyclicGraphListener;
	private BeanGraphIteratorListener rewiringGraphListener;
	private CompiledBeanIterator.CompiledBeanIteratorProvider compiledBeanIteratorProvider;
	
	@Override
	public <E> E breakCycles(E modelInstance) {
		if (modelInstance != null && compiledBeanIteratorProvider != null && acyclicGraphListener != null) {
			CompiledBeanIterator iterator = compiledBeanIteratorProvider.getCompiledBeanIteratorForType(modelInstance.getClass());
			iterator.traverse(modelInstance, acyclicGraphListener, null);
		}
		return modelInstance;
	}
	
	@Override
	public <E> E rebuildCycles(E modelInstance) {
		if (modelInstance != null && compiledBeanIteratorProvider != null && rewiringGraphListener != null) {
			CompiledBeanIterator iterator = compiledBeanIteratorProvider.getCompiledBeanIteratorForType(modelInstance.getClass());
			iterator.traverse(modelInstance, rewiringGraphListener, null);
		}
		return modelInstance;
	}
	
	public void setCompiledBeanIteratorProvider(CompiledBeanIterator.CompiledBeanIteratorProvider compiledBeanIteratorProvider) {
		this.compiledBeanIteratorProvider = compiledBeanIteratorProvider;
	}
	
	public void setAcyclicGraphListener(BeanGraphIteratorListener acyclicGraphListener) {
		this.acyclicGraphListener = acyclicGraphListener;
	}

	public void setRewiringGraphListener(BeanGraphIteratorListener rewiringGraphListener) {
		this.rewiringGraphListener = rewiringGraphListener;
	}
}
