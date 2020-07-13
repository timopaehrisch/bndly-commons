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

import org.bndly.common.reflection.PathWriter;
import org.bndly.common.reflection.PathWriterImpl;
import java.util.ArrayList;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CollectionValueWritingTest {

    @Test
    public void run() {
        String exp1 = "composed[1].name";
        String v1 = "Thomas";
        String exp2 = "composed[0].name";
        String v2 = "Timo";
        
        PathWriter writer = new PathWriterImpl();
        AClassWithACollection target = new AClassWithACollection();
        writer.write(exp1, v1, target);
        writer.write(exp2, v2, target);
        runAssertions(target, v1, v2);
        
        target = new AClassWithACollection();
        target.setComposed(new ArrayList<ComposedInACollection>());
        // this should work without an exception
        writer.write(exp1, v1, target);
        writer.write(exp2, v2, target);
        runAssertions(target, v1, v2);

    }

    private void runAssertions(AClassWithACollection target, String v1, String v2) {
        Assert.assertNotNull(target.getComposed());
        Assert.assertEquals(target.getComposed().size(), 2);
        
        ComposedInACollection element0 = target.getComposed().get(0);
        ComposedInACollection element1 = target.getComposed().get(1);
        Assert.assertEquals(element1.getName(), v1);
        Assert.assertEquals(element0.getName(), v2);
    }
}
