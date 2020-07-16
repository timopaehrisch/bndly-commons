package org.bndly.common.service.decorator;

/*-
 * #%L
 * Service Decorator
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

import org.bndly.common.service.decorator.api.ServiceDecorator;
import org.bndly.common.service.decorator.api.ServiceDecoratorChain;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InvocationServiceDecorator implements ServiceDecorator {

    @Override
    public boolean appliesTo(Method method, Object wrappedInstance) {
        return true;
    }

    @Override
    public boolean precedes(ServiceDecorator decorator) {
        // the invocation decorator wants to be the last decorator
        return false;
    }
    
    @Override
    public Object execute(ServiceDecoratorChain decoratorChain, Object invocationTarget, Method invokedMethod, Object... args) throws Throwable {
        try {
            Object returned = invokedMethod.invoke(invocationTarget, args);
            return returned;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
    
}
