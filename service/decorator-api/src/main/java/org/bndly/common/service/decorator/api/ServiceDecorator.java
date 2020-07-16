package org.bndly.common.service.decorator.api;

/*-
 * #%L
 * Service Decorator API
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

import java.lang.reflect.Method;

public interface ServiceDecorator {

	/**
	 * Depending on the passed in method a decorator should return either true or false to let the invoker know wheter this decorator should be applied to the provided method.
	 *
	 * @param method the method this decorator should wrap
	 * @param wrappedInstance the object instance that is wrapped by this decorator
	 * @return true if the provided method should be wrapped, otherwise false
	 */
	boolean appliesTo(Method method, Object wrappedInstance);

	/**
	 * This method is invoked to let service decorators decide which other decorators they want to precede in the decorator chain.
	 *
	 * @param decorator the decorator that this instance should precede
	 * @return true if the current instance shall precede the passed in decorator
	 */
	boolean precedes(ServiceDecorator decorator);

	/**
	 * If a decorator is applied to a method invocation this method is called to process the decorator. NOTE: the implementation of the decorator may decide if further 
	 * decorators shall be activated by calling doContinue on the decorator chain.
	 *
	 * @param decoratorChain the decorator chain. call doContinue to continue processing the decorator chain.
	 * @param invocationTarget the object that is decorated
	 * @param invokedMethod the method that is invoked on the decorated object
	 * @param args the arguments passed to the decorated object
	 * @return the return value of decoratorChain.doContinue() or a return value of the decorator.
	 */
	Object execute(ServiceDecoratorChain decoratorChain, Object invocationTarget, Method invokedMethod, Object... args) throws Throwable;
}
