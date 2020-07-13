package org.bndly.common.util;

/*-
 * #%L
 * Reflection
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

import org.bndly.common.reflection.ReflectionUtil;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CollectionReflectionTest {
    @Test
    public void testCollectionReflection() {
	List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(AClassForTests.class);
	for (Field field : fields) {
	    if(field.getName().equals("stringCollection")) {
		boolean result = ReflectionUtil.isCollectionFieldFillableWithObjectsInheritedOfType(AClassForTests.class, field);
		Assert.assertTrue(!result, "the field stringCollection is a List<String> field, but the isCollectionFieldFillableWithObjectsInheritedOfType method said, that the type "+AClassForTests.class.getSimpleName()+" is assignable to the list items.");
		
		result = ReflectionUtil.isCollectionFieldFillableWithObjectsInheritedOfType(String.class, field);
		Assert.assertTrue(result, "the field stringCollection is a List<String> field, but the isCollectionFieldFillableWithObjectsInheritedOfType method said, that the type "+String.class.getSimpleName()+" not applicable to the list items.");
	    
	    } else if(field.getName().equals("objectCollection")) {
		boolean result = ReflectionUtil.isCollectionFieldFillableWithObjectsInheritedOfType(String.class, field);
		Assert.assertTrue(!result, "the field objectCollection is a List<AClassForTests> field, but the isCollectionFieldFillableWithObjectsInheritedOfType method said, that the type "+String.class.getSimpleName()+" is assignable to the list items.");
		
		result = ReflectionUtil.isCollectionFieldFillableWithObjectsInheritedOfType(AClassForTests.class, field);
		Assert.assertTrue(result, "the field objectCollection is a List<AClassForTests> field, but the isCollectionFieldFillableWithObjectsInheritedOfType method said, that the type "+AClassForTests.class.getSimpleName()+" not applicable to the list items.");
	    }
	}
    }
    
    @Test
    public void testGenericTypeParameterRetrieval() {
	List<Class<?>> params = ReflectionUtil.collectGenericTypeParametersFromType(AClassThatSetsTypeParameters.class);
	Assert.assertNotNull(params);
	Assert.assertEquals(params.size(), 3);
	
	Assert.assertEquals(params.get(0).getSimpleName(), "String");
	Assert.assertEquals(params.get(1).getSimpleName(), "List");
	Assert.assertEquals(params.get(2).getSimpleName(), "Date");
	
	Assert.assertEquals(params.get(0), String.class);
	Assert.assertEquals(params.get(1), List.class);
	Assert.assertEquals(params.get(2), Date.class);
    }
    
    @Test
    public void testGenericTypeParameterWithInheritanceRetrieval() {
	AClassThatSetsParametersInDifferentTypes tmp = new AClassThatSetsParametersInDifferentTypes();
	Assert.assertEquals(tmp.getA(), Date.class);
	Assert.assertEquals(tmp.getB(), String.class);
    }
}
