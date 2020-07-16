package org.bndly.shop.common.csv.parsing;

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

import org.bndly.shop.common.csv.model.DocumentImpl;
import org.bndly.shop.common.csv.model.Row;
import org.bndly.shop.common.csv.model.RowImpl;
import java.io.StringReader;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ParsingTest {
    
    @Test
    public void runSimple() {
        String input = "hallo,welt,\"dies, ist ein test\",\"toll\"";
        DocumentImpl d = new CSVParser().parse(new StringReader(input));
        Assert.assertNotNull(d.getRows());
        Assert.assertEquals(d.getRows().size(), 1);
        Row row = d.getRows().get(0);
        Assert.assertNotNull(row.getValues());
        Assert.assertEquals(row.getValues().size(), 4);
		Assert.assertEquals(row.getValues().get(0).getRaw(), "hallo");
		Assert.assertEquals(row.getValues().get(1).getRaw(), "welt");
		Assert.assertEquals(row.getValues().get(2).getRaw(), "dies, ist ein test");
		Assert.assertEquals(row.getValues().get(3).getRaw(), "toll");
    }
    
	@Test
    public void runEscapedQuotes() {
        String input = "hallo,welt,\"dies, ist ein test\",\"\"\"toll\"\"\"";
        DocumentImpl d = new CSVParser().parse(new StringReader(input));
        Assert.assertNotNull(d.getRows());
        Assert.assertEquals(d.getRows().size(), 1);
        Row row = d.getRows().get(0);
        Assert.assertNotNull(row.getValues());
        Assert.assertEquals(row.getValues().size(), 4);
		Assert.assertEquals(row.getValues().get(0).getRaw(), "hallo");
		Assert.assertEquals(row.getValues().get(1).getRaw(), "welt");
		Assert.assertEquals(row.getValues().get(2).getRaw(), "dies, ist ein test");
		Assert.assertEquals(row.getValues().get(3).getRaw(), "\"toll\"");
    }
    
    @Test
    public void runMultipleRows() {
        String input = "hallo,welt\r\ndies ist ein, test";
        DocumentImpl d = new CSVParser().parse(new StringReader(input));
        Assert.assertNotNull(d.getRows());
        Assert.assertEquals(d.getRows().size(), 2);
        Row row = d.getRows().get(0);
        Assert.assertNotNull(row.getValues());
        Assert.assertEquals(row.getValues().size(), 2);
		Assert.assertEquals(row.getValues().get(0).getRaw(), "hallo");
		Assert.assertEquals(row.getValues().get(1).getRaw(), "welt");
        row = d.getRows().get(1);
        Assert.assertNotNull(row.getValues());
        Assert.assertEquals(row.getValues().size(), 2);
		Assert.assertEquals(row.getValues().get(0).getRaw(), "dies ist ein");
		Assert.assertEquals(row.getValues().get(1).getRaw(), " test");
    }
    
    @Test
    public void runMultipleRowsWithNewLineInValue() {
        String input = "hallo,welt\r\n\"dies ist \nein mehrzeiliger\", test";
        DocumentImpl d = new CSVParser().parse(new StringReader(input));
        Assert.assertNotNull(d.getRows());
        Assert.assertEquals(d.getRows().size(), 2);
        Row row = d.getRows().get(0);
        Assert.assertNotNull(row.getValues());
        Assert.assertEquals(row.getValues().size(), 2);
		Assert.assertEquals(row.getValues().get(0).getRaw(), "hallo");
		Assert.assertEquals(row.getValues().get(1).getRaw(), "welt");
        row = d.getRows().get(1);
        Assert.assertNotNull(row.getValues());
        Assert.assertEquals(row.getValues().size(), 2);
		Assert.assertEquals(row.getValues().get(0).getRaw(), "dies ist \nein mehrzeiliger");
		Assert.assertEquals(row.getValues().get(1).getRaw(), " test");
    }
    
}
