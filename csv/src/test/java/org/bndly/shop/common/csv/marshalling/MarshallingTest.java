package org.bndly.shop.common.csv.marshalling;

/*-
 * #%L
 * CSV
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

import org.bndly.shop.common.csv.CSVMarshallingException;
import org.bndly.shop.common.csv.model.DocumentImpl;
import org.bndly.shop.common.csv.serializing.CSVSerializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class MarshallingTest {

    public static class TestClass {

        private String name;
        private List<TestItem> items;
        private Long id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<TestItem> getItems() {
            return items;
        }

        public void setItems(List<TestItem> items) {
            this.items = items;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class TestItem {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testSimple() throws IOException, CSVMarshallingException {
        TestClass t = new TestClass();
        t.setName("test object");
        t.setId(1L);
        t.setItems(new ArrayList<MarshallingTest.TestItem>());
        TestItem item = new TestItem();
        item.setName("test item");
        t.getItems().add(item);
        item = new TestItem();
        item.setName("test item2");
        t.getItems().add(item);
        
//        DocumentImpl document = new Marshaller().marshall(t);
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		new CSVSerializer().serialize(document, bos, "UTF-8");
//		bos.flush();
//        String csvText = bos.toString("UTF-8");
    }
}
