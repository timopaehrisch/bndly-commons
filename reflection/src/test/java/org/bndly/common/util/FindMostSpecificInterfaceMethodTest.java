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
import org.bndly.common.util.FooInterface.Bar;
import org.bndly.common.util.FooInterface.Bazzz;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FindMostSpecificInterfaceMethodTest {
    
    @Test
    public void runAmbigouityTest() throws NoSuchMethodException {
        Method genericMethod = GenericInterface.class.getDeclaredMethod("doSomethingWith", Object.class);
        Method specificMethod = FooInterface.class.getDeclaredMethod("doSomethingWith", Bar.class);
        Method ambigiousMethod = FooInterface.class.getDeclaredMethod("doSomethingWith", Bazzz.class);
        Assert.assertNotNull(genericMethod);
        Assert.assertNotNull(specificMethod);
        Assert.assertNotNull(ambigiousMethod);
        try {
			Method r = ReflectionUtil.findMostSpecificInterfaceMethod(FooInstance.class, genericMethod);
            Assert.fail("expected an exception because the ambigouity can not be resolved.");
        } catch(Exception e) {
        }
    }
    
    @Test
    public void runWorkingTest() throws NoSuchMethodException {
        Method genericMethod = GenericInterface.class.getDeclaredMethod("makeLoveWith", Object.class);
        Method specificMethod = FooInterface.class.getDeclaredMethod("makeLoveWith", Bar.class);
        Assert.assertNotNull(genericMethod);
        Assert.assertNotNull(specificMethod);
        Method foundMethod = ReflectionUtil.findMostSpecificInterfaceMethod(FooInstance.class, genericMethod);
        Assert.assertEquals(foundMethod, specificMethod);
    }
}
