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
import java.util.List;

public class AClassThatAddsAnotherTypeParameter<B, A> extends AClassWithOneTypeParameter<A> {
    private Class<?> b;

    public AClassThatAddsAnotherTypeParameter() {
	super();
	init();
    }

    
    public Class<?> getB() {
	return b;
    }
    
    private void init(){
	List<Class<?>> params = ReflectionUtil.collectGenericTypeParametersFromType(getClass());
	b = params.get(0);
	a = params.get(1);
    }
    
}
