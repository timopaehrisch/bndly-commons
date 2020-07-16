package org.bndly.common.service.decorator.wrapper;

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

import org.bndly.common.service.decorator.InvocationServiceDecorator;
import org.bndly.common.service.decorator.api.ServiceDecorator;
import org.bndly.common.service.decorator.api.ServiceDecoratorChain;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class InterfaceMethodInvocationTest {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface SampleAnnotation {
		
	}
	
	public static interface Model {

	}

	public static interface Generic<A> {

		Model doStuff(A parameter);
	}

	public static interface Specific extends Generic<Model> {

		@Override
		public Model doStuff(Model parameter);
		
	}

	public static interface Custom {

		@SampleAnnotation
		Model doStuffAnnotated(Model parameter);
	}

	public static interface Complete extends Specific, Custom {
	}

	@Test
	public void invokeTestMethod() {
		final Model model0 = new Model() { };
		final Model model1 = new Model() { };
		final Model model2 = new Model() { };
		Complete complete = new Complete() {
			@Override
			public Model doStuff(Model parameter) {
				return parameter;
			}

			@Override
			public Model doStuffAnnotated(Model parameter) {
				return model2;
			}
			
		};
		final int[] tmp = new int[]{0};
		ServiceProxyFactoryImpl serviceProxyFactoryImpl = new ServiceProxyFactoryImpl();
		ServiceDecorator decorator = new ServiceDecorator() {
			@Override
			public boolean appliesTo(Method method, Object wrappedInstance) {
				return method.isAnnotationPresent(SampleAnnotation.class);
			}

			@Override
			public boolean precedes(ServiceDecorator decorator) {
				return true;
			}

			@Override
			public Object execute(ServiceDecoratorChain decoratorChain, Object invocationTarget, Method invokedMethod, Object... args) throws Throwable {
				try {
					return decoratorChain.doContinue();
				} finally {
					tmp[0] = tmp[0] + 1;
				}
			}
		};
		serviceProxyFactoryImpl.registerDecorator(decorator);
		serviceProxyFactoryImpl.registerDecorator(new InvocationServiceDecorator());
		Complete decorated = serviceProxyFactoryImpl.decorateService(complete, Complete.class);
		
		Assert.assertEquals(tmp[0], 0);
		Assert.assertEquals(decorated.doStuff(model0), model0);
		Assert.assertEquals(tmp[0], 0);
		Assert.assertEquals(decorated.doStuffAnnotated(model0), model2);
		Assert.assertEquals(tmp[0], 1);
		Assert.assertEquals(decorated.doStuffAnnotated(model1), model2);
		Assert.assertEquals(tmp[0], 2);
		Assert.assertEquals(decorated.doStuff(model1), model1);
		Assert.assertEquals(tmp[0], 2);
		
		serviceProxyFactoryImpl.unregisterDecorator(decorator);
		Assert.assertEquals(decorated.doStuffAnnotated(model1), model2);
		Assert.assertEquals(tmp[0], 3); // the decorator is still being used, because the decorated service does not make use of tracking.
		
		tmp[0] = 0;
		Complete decorated2 = serviceProxyFactoryImpl.decorateServiceWithDecoratorTracking(complete, Complete.class);
		Assert.assertEquals(decorated2.doStuffAnnotated(model0), model2);
		Assert.assertEquals(tmp[0], 0);
		serviceProxyFactoryImpl.registerDecorator(decorator);
		Assert.assertEquals(decorated2.doStuffAnnotated(model0), model2);
		Assert.assertEquals(tmp[0], 1);
		
		serviceProxyFactoryImpl.untrackService(decorated2);
		
		Assert.assertEquals(decorated2.doStuffAnnotated(model0), model2);
		Assert.assertEquals(tmp[0], 2);
		
		serviceProxyFactoryImpl.unregisterDecorator(decorator);
		
		// the decorator will still be called, even though it is unregistered, because the decorated2 instance has been untracked.
		Assert.assertEquals(decorated2.doStuffAnnotated(model0), model2);
		Assert.assertEquals(tmp[0], 3);
	}
}
