package org.bndly.rest.links;

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

import org.bndly.rest.atomlink.impl.DefaultELContext;
import org.bndly.rest.atomlink.impl.DefaultFunctionMapper;
import org.bndly.rest.atomlink.impl.DefaultVariableMapper;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ELTest {
	
	@Test
	public void test() {
		final ExpressionFactory f = ExpressionFactory.newInstance();
//		ValueExpression exp = f.createValueExpression("${abc}", String.class);
		final CompositeELResolver composite = new CompositeELResolver();
		composite.add(new BeanELResolver());
		composite.add(new ArrayELResolver());
		composite.add(new MapELResolver());
		composite.add(new ListELResolver());
		composite.add(new ResourceBundleELResolver());
		FunctionMapper functionMapper = new DefaultFunctionMapper();
		DefaultVariableMapper variableMapper = new DefaultVariableMapper();
		DefaultELContext context = new DefaultELContext(composite, functionMapper, variableMapper);

		variableMapper.setVariable("aaa", f.createValueExpression("testString", String.class));
		ValueExpression exp = f.createValueExpression(context, "${aaa}WithSuffix", String.class);
		Object v = exp.getValue(context);
//		Object v = exp.getValue(context);
		Assert.assertEquals(v,"testStringWithSuffix");
		
		exp = f.createValueExpression(context, "${notfound}", String.class);
		try {
			exp.getValue(context);
			Assert.fail("not existing property was resolved to something");
		} catch(PropertyNotFoundException e) {
		}
	}
}
