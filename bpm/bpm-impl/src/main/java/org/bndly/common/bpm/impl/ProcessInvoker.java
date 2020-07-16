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

import org.bndly.common.bpm.annotation.Context;
import org.bndly.common.bpm.annotation.Event;
import org.bndly.common.bpm.annotation.ProcessID;
import org.bndly.common.bpm.annotation.ProcessVariable;
import org.bndly.common.bpm.annotation.Resume;
import org.bndly.common.bpm.annotation.ReturnVariable;
import org.bndly.common.bpm.annotation.Signal;
import org.bndly.common.bpm.api.ContextResolver;
import org.bndly.common.bpm.api.ProcessInstanceService;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bndly.common.bpm.annotation.ProcessDefinition;

public class ProcessInvoker implements InvocationHandler {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessInvoker.class);
	public static final String UNIQUE_PROCESS_INSTANCE_ID_VAR = "__uniqueProcessInstanceId";
	public static final String PROCESS_INSTANCE_PROCESS_NAME_VAR = "__processName";
	private static final String METHOD_PREFIX_RUN = "run";
	private static final String METHOD_PREFIX_RESUME = "resume";
	
	private ProcessInvokerFactoryImpl factory;
	private final Map<Method, CompiledMethod> compiledMethods = new HashMap<>();


	private static enum InvocationMode {

		RUN,
		RESUME
	}
	
	private static interface CompiledMethod {
		Object invoke(Object target, Object[] arguments) throws Throwable;
	}
	
	private static interface CompiledVariableProducer {
		void produceVariables(Object[] arguments, Collection<org.bndly.common.bpm.api.ProcessVariable> variables);
	}
	
	private static interface CompiledValueProvider<E> {
		E get(Object[] arguments);
	}
	private static interface CompiledContext {
		void set(Object[] arguments, ContextResolver contextResolver);
	}
	
	@Override
	public Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
		CompiledMethod compiledMethod = compiledMethods.get(method);
		if (compiledMethod == null) {
			compiledMethod = compile(method);
			compiledMethods.put(method, compiledMethod);
		}
		return compiledMethod.invoke(target, arguments);
	}
	
	private CompiledMethod compile(final Method method) {
		// stuff like hashCode and toString
		if (method.getDeclaringClass().equals(Object.class)) {
			return new CompiledMethod() {
				@Override
				public Object invoke(Object target, Object[] arguments) throws Throwable {
					return method.invoke(this, arguments);
				}
			};
		}

		final boolean debug = LOG.isDebugEnabled();
		final long s;
		if (debug) {
			s = System.currentTimeMillis();
		} else {
			s = -1;
		}
		try {
			// get the process name and the mode to either run or resume a process instance
			InvocationMode mode = InvocationMode.RUN;
			String methodName = method.getName();
			String tmpProcessName = methodName;
			if (tmpProcessName.startsWith(METHOD_PREFIX_RUN)) {
				tmpProcessName = tmpProcessName.substring(METHOD_PREFIX_RUN.length());
				mode = InvocationMode.RUN;
			} else if (tmpProcessName.startsWith(METHOD_PREFIX_RESUME)) {
				tmpProcessName = tmpProcessName.substring(METHOD_PREFIX_RESUME.length());
				mode = InvocationMode.RESUME;
			}
			if (method.getAnnotation(Resume.class) != null) {
				mode = InvocationMode.RESUME;
			}
			ProcessDefinition processAnnotation = method.getAnnotation(org.bndly.common.bpm.annotation.ProcessDefinition.class);
			if (processAnnotation == null) {
				processAnnotation = method.getDeclaringClass().getAnnotation(org.bndly.common.bpm.annotation.ProcessDefinition.class);
			}
			if (processAnnotation != null) {
				tmpProcessName = processAnnotation.value();
			}
			final String processName = tmpProcessName;
			
			// if we resume a process instance, we might be interested in a @Signal annotation
			Signal signalAnnotation = method.getAnnotation(Signal.class);
			
			final ContextResolver contextResolver = factory.getContextResolver();
			String tmpReturnVariableName = null;

			ReturnVariable rv = method.getAnnotation(ReturnVariable.class);
			if (rv != null) {
				tmpReturnVariableName = rv.value();
				if ("".equals(tmpReturnVariableName)) {
					tmpReturnVariableName = null;
				}
			}
			final String returnVariableName = tmpReturnVariableName;
			final Class<?> returnType = method.getReturnType();
			final Collection<CompiledVariableProducer> variableProducers = new ArrayList<>();
			final Collection<CompiledContext> compiledContexts = new ArrayList<>();
			CompiledValueProvider<String> tmpProcessId = null;
			CompiledValueProvider<String> tmpEventName = null;
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();
			Class<?>[] parameterTypes = method.getParameterTypes();
			int i = 0;
			for (Annotation[] annotations : parameterAnnotations) {
				final int index = i;
				final Class parameterType = parameterTypes[i];
				final CompiledValueProvider vp = new CompiledValueProvider() {
					@Override
					public Object get(Object[] arguments) {
						Object parameterValue = arguments[index];
						return parameterValue;
					}
				};
				for (Annotation annotation : annotations) {
					if (ProcessVariable.class.isAssignableFrom(annotation.getClass())) {
						final ProcessVariable pvA = ProcessVariable.class.cast(annotation);
						variableProducers.add(new CompiledVariableProducer() {
							@Override
							public void produceVariables(Object[] arguments, Collection<org.bndly.common.bpm.api.ProcessVariable> variables) {
								Object parameterValue = vp.get(arguments);
								if (parameterValue != null) {
									variables.add(new org.bndly.common.bpm.impl.ProcessVariableImpl(pvA.name(), parameterValue));
								}
							}
						});
					} else if (ProcessID.class.isAssignableFrom(annotation.getClass())) {
						if (String.class.equals(parameterType)) {
							tmpProcessId = vp;
						} else {
							throw new IllegalArgumentException("@ProcessID should be assigned to parameters with type String. see " + method.toString());
						}
					} else if (Event.class.isAssignableFrom(annotation.getClass())) {
						if (String.class.equals(parameterType)) {
							tmpEventName = vp;
						} else {
							throw new IllegalArgumentException("@Event should be assigned to parameters with type String. see " + method.toString());
						}
					} else if (Context.class.isAssignableFrom(annotation.getClass())) {
						compiledContexts.add(new CompiledContext() {
							@Override
							public void set(Object[] arguments, ContextResolver contextResolver) {
								Object parameterValue = vp.get(arguments);
								contextResolver.setContext(parameterType, parameterValue);
							}
						});
					}
				}
				i++;
			}

			final CompiledValueProvider<String> processId = tmpProcessId;
			final CompiledValueProvider<String> eventName = tmpEventName;
			if (eventName != null && signalAnnotation != null) {
				throw new IllegalStateException("The method signature may contain either arguments annotated with @Event or the method is annotated with @Signal but not both at the same time.");
			}

			final ProcessInstanceService processInstanceService = factory.getProcessInstanceService(this);
			if (InvocationMode.RUN.equals(mode)) {
				return new CompiledMethod() {
					@Override
					public Object invoke(Object target, Object[] arguments) throws Throwable {
						try {
							for (CompiledContext compiledContext : compiledContexts) {
								compiledContext.set(arguments, contextResolver);
							}
							DefaultReturnValueHandler rvh = new DefaultReturnValueHandler(returnVariableName, returnType);
							Collection<org.bndly.common.bpm.api.ProcessVariable> variables;
							if (variableProducers.isEmpty()) {
								variables = Collections.EMPTY_LIST;
							} else {
								variables = new ArrayList<>(variableProducers.size());
								for (CompiledVariableProducer variableProducer : variableProducers) {
									variableProducer.produceVariables(arguments, variables);
								}
							}
							processInstanceService.startProcess(processName, variables, rvh);
							return rvh.getReturnedValue();
						} finally {
							contextResolver.clear();
						}
					}
				};
			} else if (InvocationMode.RESUME.equals(mode)) {
				if (processId == null) {
					throw new IllegalArgumentException("missing method parameter with @ProcessId annotation");
				}
				if (eventName != null) {
					return new CompiledMethod() {
						@Override
						public Object invoke(Object target, Object[] arguments) throws Throwable {
							try {
								for (CompiledContext compiledContext : compiledContexts) {
									compiledContext.set(arguments, contextResolver);
								}
								DefaultReturnValueHandler rvh = new DefaultReturnValueHandler(returnVariableName, returnType);
								String pid = processId.get(arguments);
								if (pid == null) {
									throw new IllegalArgumentException("provided process id was null");
								}
								org.bndly.common.bpm.impl.ProcessInstanceImpl instance = new org.bndly.common.bpm.impl.ProcessInstanceImpl(pid, processName);
								Collection<org.bndly.common.bpm.api.ProcessVariable> variables;
								if (variableProducers.isEmpty()) {
									variables = Collections.EMPTY_LIST;
								} else {
									variables = new ArrayList<>(variableProducers.size());
									for (CompiledVariableProducer variableProducer : variableProducers) {
										variableProducer.produceVariables(arguments, variables);
									}
								}
								instance.setVariables(variables);
								processInstanceService.resumeProcess(instance, eventName.get(arguments), rvh);
								return rvh.getReturnedValue();
							} finally {
								contextResolver.clear();
							}
						}
					};
				} else if (signalAnnotation != null) {
					final String signaledActivity = signalAnnotation.activity();
					final String signaledMessage = signalAnnotation.message();
					return new CompiledMethod() {
						@Override
						public Object invoke(Object target, Object[] arguments) throws Throwable {
							try {
								for (CompiledContext compiledContext : compiledContexts) {
									compiledContext.set(arguments, contextResolver);
								}
								DefaultReturnValueHandler rvh = new DefaultReturnValueHandler(returnVariableName, returnType);
								String pid = processId.get(arguments);
								if (pid == null) {
									throw new IllegalArgumentException("provided process id was null");
								}
								org.bndly.common.bpm.impl.ProcessInstanceImpl instance = new org.bndly.common.bpm.impl.ProcessInstanceImpl(pid, processName);
								Collection<org.bndly.common.bpm.api.ProcessVariable> variables;
								if (variableProducers.isEmpty()) {
									variables = Collections.EMPTY_LIST;
								} else {
									variables = new ArrayList<>(variableProducers.size());
									for (CompiledVariableProducer variableProducer : variableProducers) {
										variableProducer.produceVariables(arguments, variables);
									}
								}
								instance.setVariables(variables);
								processInstanceService.resumeProcess(instance, signaledActivity, signaledMessage, rvh);
								return rvh.getReturnedValue();
							} finally {
								contextResolver.clear();
							}
						}
					};
				} else {
					throw new IllegalStateException("methods that resume a process either need an @Event annotated parameter or an @Signal annotation");
				}
			} else {
				throw new IllegalStateException("unsupported invocation mode: " + mode);
			}
		} finally {
			if (debug) {
				final long e = System.currentTimeMillis();
				final long d = e - s;
				LOG.debug("compiling of process invocation via {}.{} took {}ms", method.getDeclaringClass().getSimpleName(), method.getName(), d);
			}
		}
	}

	public void setProcessInvokerFactoryImpl(ProcessInvokerFactoryImpl factory) {
		if (factory == null) {
			throw new IllegalArgumentException("factory is not allowed to be null");
		}
		this.factory = factory;
	}
}
