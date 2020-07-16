package org.bndly.shop.common.csv.serializing;

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

import org.bndly.shop.common.csv.CSVConfig;
import org.bndly.shop.common.csv.CSVException;
import org.bndly.shop.common.csv.model.DocumentImpl;
import org.bndly.shop.common.csv.model.RowImpl;
import org.bndly.shop.common.csv.model.ValueImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SerializingTest {
    
    @Test
    public void testSimple() throws IOException, CSVException {
        DocumentImpl d = new DocumentImpl();
        RowImpl row = createRowInDocument(d);
        addValueToRow("hallo", row);
        addValueToRow("welt", row);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new CSVSerializer().serialize(d, bos, "UTF-8");
		bos.flush();
        String csvText = bos.toString("UTF-8");
        Assert.assertEquals(csvText, "hallo,welt");
    }
    
    @Test
    public void testQuoted() throws IOException, CSVException {
        DocumentImpl d = new DocumentImpl();
        RowImpl row = createRowInDocument(d);
        addValueToRow("\"hallo\"", row);
        addValueToRow("welt", row);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new CSVSerializer().serialize(d, bos, "UTF-8");
        bos.flush();
        String csvText = bos.toString("UTF-8");
        Assert.assertEquals(csvText, "\"\"\"hallo\"\"\",welt");
    }
    
    @Test
    public void testNewLine() throws IOException, CSVException {
        DocumentImpl d = new DocumentImpl();
        RowImpl row = createRowInDocument(d);
        addValueToRow("hallo\r\nmehrzeilige", row);
        addValueToRow("welt", row);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new CSVSerializer().serialize(d, bos, "UTF-8");
		bos.flush();
        String csvText = bos.toString("UTF-8");
        Assert.assertEquals(csvText, "\"hallo\r\nmehrzeilige\",welt");
    }
	
	@Test
	public void testRequiresQuotes() {
		Assert.assertTrue(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\r\n"));
		Assert.assertTrue(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\""));
		Assert.assertTrue(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "test\r\n"));
		Assert.assertTrue(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "test\""));
		Assert.assertTrue(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\r\ntest"));
		Assert.assertTrue(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\"test"));
		Assert.assertFalse(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, ""));
		Assert.assertFalse(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "test"));
		Assert.assertFalse(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\r"));
		Assert.assertFalse(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\t"));
		Assert.assertFalse(ValueImpl.requiresQuotes(CSVConfig.DEFAULT, "\n"));
	}

    private RowImpl createRowInDocument(DocumentImpl document) {
        RowImpl row = new RowImpl(document, document.getRows().size());
        document.addRow(row);
        return row;
    }
    
    private void addValueToRow(String rawValue, RowImpl row) {
        ValueImpl v = new ValueImpl(rawValue, row, row.getValues().size(), ValueImpl.requiresQuotes(CSVConfig.DEFAULT, rawValue));
        row.addValue(v);
    }
    
}
