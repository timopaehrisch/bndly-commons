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

import org.bndly.common.service.shared.api.GenericResourceService;
import org.bndly.common.service.shared.api.Logged;
import org.bndly.common.service.decorator.api.ServiceDecorator;
import org.bndly.common.service.decorator.api.ServiceDecoratorChain;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingServiceDecorator implements ServiceDecorator {

	private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceDecorator.class);

	@Override
	public boolean appliesTo(Method method, Object wrappedInstance) {
		Class<?> declaringClass = method.getDeclaringClass();
		return method.isAnnotationPresent(Logged.class) || declaringClass.isAnnotationPresent(Logged.class) || GenericResourceService.class.isAssignableFrom(declaringClass);
	}

	@Override
	public boolean precedes(ServiceDecorator decorator) {
		// the logging decorator should be placed at the very beginning of the decorator chain
		return true;
	}

	@Override
	public Object execute(ServiceDecoratorChain decoratorChain, Object invocationTarget, Method invokedMethod, Object... args) throws Throwable {
		if (LOG.isInfoEnabled()) {
			StopWatch sw = new StopWatch();
			sw.start();
			try {
				Object result = decoratorChain.doContinue();
				return result;
			} finally {
				sw.stop();
				String sn = invokedMethod.getDeclaringClass().getSimpleName() + "." + invokedMethod.getName();
				sn = String.format("%1$-" + 70 + "s", sn);
				String ms = sw.getTotalTimeMillis() + "ms";
				ms = String.format("%1$" + 5 + "s", ms);
				LOG.info("{} took {}", new Object[]{sn, ms});
			}
		} else {
			return decoratorChain.doContinue();
		}
	}

}
