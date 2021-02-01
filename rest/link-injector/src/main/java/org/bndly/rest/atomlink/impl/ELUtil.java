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

import de.odysseus.el.util.SimpleResolver;
import org.bndly.rest.api.Context;

import java.lang.reflect.Method;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.PropertyNotFoundException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ELUtil {

	private ELUtil() {
	}

	public static ELContext createELContext(ExpressionFactory expressionFactory) {
		FunctionMapper functionMapper = new DefaultFunctionMapper();
		DefaultVariableMapper variableMapper = new DefaultVariableMapper();
		DefaultELContext context = new DefaultELContext(new SimpleResolver(true), functionMapper, variableMapper);
		injectExpressionFactory(context, expressionFactory);
		return context;
	}

	public static ELContext createELContext(Object objectForThisVariable, Class<?> typeOfThisVariable, ExpressionFactory expressionFactory, Context ctx) {
		ELContext context = createELContext(objectForThisVariable, typeOfThisVariable, expressionFactory);
		context.getVariableMapper().setVariable("ctx", expressionFactory.createValueExpression(ctx, Context.class));
		return context;
	}

	public static ELContext createELContext(Object objectForThisVariable, Class<?> typeOfThisVariable, ExpressionFactory expressionFactory) {
		FunctionMapper functionMapper = new DefaultFunctionMapper();
		DefaultVariableMapper variableMapper = new DefaultVariableMapper();
		DefaultELContext context = new DefaultELContext(new SimpleResolver(true), functionMapper, variableMapper);
		variableMapper.setVariable("this", expressionFactory.createValueExpression(objectForThisVariable, typeOfThisVariable));
		injectExpressionFactory(context, expressionFactory);
		return context;
	}

	private static void injectExpressionFactory(DefaultELContext context, ExpressionFactory expressionFactory) {
		context.putContext(ExpressionFactory.class, expressionFactory);
	}

	public static Object evaluateEL(Method m, ELContext context, String expression, ExpressionFactory expressionFactory) {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(context.getClass().getClassLoader());
				return expressionFactory.createValueExpression(context, expression, Object.class).getValue(context);
			} finally {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		} catch (PropertyNotFoundException x) {
			throw x; // if a property can not be found, this might be, because the expression is designed for a different type, that can be inserted, into the field
		} catch (Exception x) {
			throw new ServiceDiscoveryException(m, "Failed to evaluate EL expression: " + expression, x);
		}
	}

	public static Boolean evaluateELBoolean(Method m, ELContext context, String expression, ExpressionFactory expressionFactory) {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(context.getClass().getClassLoader());
				Boolean r = (Boolean) expressionFactory.createValueExpression(context, expression, Boolean.class).getValue(context);
				return r;
			} finally {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		} catch (Exception x) {
			throw new ServiceDiscoveryException(m, "Failed to evaluate EL expression: " + expression, x);
		}
	}
}
