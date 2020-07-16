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
import org.bndly.common.bpm.api.ProcessVariable;
import org.bndly.common.bpm.api.ProcessVariableAdapter;
import org.bndly.common.bpm.api.ProcessVariableType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.variable.ValueFields;
import org.activiti.engine.impl.variable.VariableType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = {JavaDelegate.class, ProcessVariableAdapter.class, VariableType.class, ProcessVariableType.class},
		property = {
			OSGIConstants.IS_GLOBAL_DELEGATE + ":Boolean=true",
			OSGIConstants.DELEGATE_NAME + "=osgi"
		}
)
public class GetOSGIServiceJavaDelegate implements JavaDelegate, ProcessVariableAdapter, VariableType, ProcessVariableType {

	private Expression varExpression;
	private Expression filterExpression;
	private Expression classExpression;
	private BundleContext bundleContext;

	@Override
	public boolean doesSupport(ProcessVariable variable) {
		Object val = variable.getValue();
		return bundleContext != null && OSGIServiceHolder.class.isInstance(val);
	}

	@Override
	public String getType(ProcessVariable variable) {
		return "osgi";
	}

	@Override
	public Object getValue(ValueFields valueFields) {
		BundleContext ctx = bundleContext;
		if (ctx == null) {
			return null;
		}
		String className = valueFields.getTextValue();
		if (className == null) {
			return null;
		}
		String filter = valueFields.getTextValue2();
		return new OSGIServiceHolder(className, filter);
	}

	@Override
	public void setValue(Object value, ValueFields valueFields) {
		if (OSGIServiceHolder.class.isInstance(value)) {
			OSGIServiceHolder serviceHolder = (OSGIServiceHolder) value;
			valueFields.setTextValue(serviceHolder.className);
			valueFields.setTextValue2(serviceHolder.filter);
		}
	}

	@Override
	public boolean isAbleToStore(Object value) {
		return OSGIServiceHolder.class.isInstance(value);
	}

	@Override
	public boolean isCachable() {
		return true;
	}

	@Override
	public String getTypeName() {
		return "OSGIServiceType";
	}
	
	
	public class OSGIServiceHolder {
		private final String className;
		private final String filter;
		private Object instance;

		public OSGIServiceHolder(String className, String filter) {
			this.className = className;
			this.filter = filter;
		}

		public Object getService() throws InvalidSyntaxException {
			if (instance == null) {
				BundleContext ctx = bundleContext;
				if (ctx == null) {
					return null;
				}
				if (filter == null) {
					ServiceReference<?> ref = ctx.getServiceReference(className);
					if (ref != null) {
						instance = ctx.getService(ref);
					}
				} else {
					ServiceReference<?>[] refs = ctx.getServiceReferences(className, filter);
					for (ServiceReference<?> ref : refs) {
						instance = ctx.getService(ref);
						if (instance != null) {
							break;
						}
					}
				}
			}
			return instance;
		}
	}
	
	@Activate
	public void activate(ComponentContext componentContext) {
		bundleContext = componentContext.getBundleContext();
	}
	@Deactivate
	public void deactivate() {
		bundleContext = null;
	}
	
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		BundleContext ctx = bundleContext;
		if (varExpression == null || classExpression == null || ctx == null) {
			return;
		}
		Object classExp = classExpression.getValue(execution);
		if (!String.class.isInstance(classExp)) {
			return;
		}
		Object varExp = varExpression.getValue(execution);
		if (!String.class.isInstance(varExp)) {
			return;
		}
		final String var = (String) varExp;
		final String className = (String) classExp;
		final String filter;
		if (filterExpression != null) {
			Object filterExp = filterExpression.getValue(execution);
			if (!String.class.isInstance(filterExp)) {
				filter = null;
			} else {
				filter = (String) filterExp;
			}
		} else {
			filter = null;
		}
		OSGIServiceHolder serviceHolder = new OSGIServiceHolder(className, filter);
		// set the service to a variable.
		// it can not be null here.
		execution.setVariable(var, serviceHolder);
	}

	public void setFilter(Expression filterExpression) {
		this.filterExpression = filterExpression;
	}

	public void setClass(Expression classExpression) {
		this.classExpression = classExpression;
	}

	public void setVar(Expression varExpression) {
		this.varExpression = varExpression;
	}
	
}
