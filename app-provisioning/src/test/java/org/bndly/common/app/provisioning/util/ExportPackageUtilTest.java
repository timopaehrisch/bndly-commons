package org.bndly.common.app.provisioning.util;

/*-
 * #%L
 * App Provisioning
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ExportPackageUtilTest {

	private CollectingConsumer collectingConsumer;

	private static class CollectingConsumer implements ExportPackageUtil.PackageConsumer {

		Map<String, List<String>> packagesWithMetaData = new LinkedHashMap<>();
		
		@Override
		public void consumePackage(String packageName, List<String> metaData) {
			packagesWithMetaData.put(packageName, new ArrayList<>(metaData));
		}
		
	}
	
	@Test
	public void testParsing() {
		collectingConsumer = new CollectingConsumer();
		Map<String, List<String>> packagesWithMetaData = collectingConsumer.packagesWithMetaData;
		String example0 = "org.jboss.logging;uses:=\"org.jboss.logmanager,org.apache.logging.log4j,org.apache.logging.log4j.spi,org.apache.logging.log4j.message,org.apache.log4j,org.slf4j.spi,org.slf4j\";version=\"3.3.0.Final\"";
		ExportPackageUtil.parse(example0, collectingConsumer);
		Assert.assertEquals(1, packagesWithMetaData.size());
		assertPackage("org.jboss.logging", "uses:=\"org.jboss.logmanager,org.apache.logging.log4j,org.apache.logging.log4j.spi,org.apache.logging.log4j.message,org.apache.log4j,org.slf4j.spi,org.slf4j\"", "version=\"3.3.0.Final\"");

		packagesWithMetaData.clear();
		String example1 = "javax.servlet.jsp.jstl.tlv;uses:=\"javax.xml.parsers,javax.servlet.jsp.tagext,org.xml.sax.helpers,org.xml.sax\";version=\"1.2.1\",javax.servlet.jsp.jstl.fmt;uses:=\"javax.servlet,javax.servlet.jsp.jstl.core,javax.servlet.jsp,javax.servlet.http\";version=\"1.2.1\",javax.servlet.jsp.jstl.core;uses:=\"javax.servlet,javax.el,javax.servlet.jsp.tagext,javax.servlet.jsp,javax.servlet.http\";version=\"1.2.1\",javax.servlet.jsp.jstl.sql;version=\"1.2.1\"";
		ExportPackageUtil.parse(example1, collectingConsumer);
		Assert.assertEquals(4, packagesWithMetaData.size());
		assertPackage("javax.servlet.jsp.jstl.tlv", "uses:=\"javax.xml.parsers,javax.servlet.jsp.tagext,org.xml.sax.helpers,org.xml.sax\"", "version=\"1.2.1\"");
		assertPackage("javax.servlet.jsp.jstl.fmt", "uses:=\"javax.servlet,javax.servlet.jsp.jstl.core,javax.servlet.jsp,javax.servlet.http\"", "version=\"1.2.1\"");
		assertPackage("javax.servlet.jsp.jstl.core", "uses:=\"javax.servlet,javax.el,javax.servlet.jsp.tagext,javax.servlet.jsp,javax.servlet.http\"", "version=\"1.2.1\"");
		assertPackage("javax.servlet.jsp.jstl.sql", "version=\"1.2.1\"");

		packagesWithMetaData.clear();
		String example2 = "org.apache.felix.scr.component;version=\"1.1.0\";uses:=\"org.osgi.service.component\",org.apache.felix.scr.info;version=\"1.0.0\",org.osgi.service.component;version=\"1.3\";uses:=\"org.osgi.framework\",org.osgi.service.component.runtime;version=\"1.3\";uses:=\"org.osgi.framework,org.osgi.service.component.runtime.dto,org.osgi.util.promise\",org.osgi.service.component.runtime.dto;version=\"1.3\";uses:=\"org.osgi.dto,org.osgi.framework.dto\",org.osgi.util.function;version=\"1.0\",org.osgi.util.promise;version=\"1.0\";uses:=\"org.osgi.util.function\"";
		ExportPackageUtil.parse(example2, collectingConsumer);
		Assert.assertEquals(7, packagesWithMetaData.size());
		assertPackage("org.apache.felix.scr.component", "version=\"1.1.0\"", "uses:=\"org.osgi.service.component\"");
		assertPackage("org.apache.felix.scr.info", "version=\"1.0.0\"");
		assertPackage("org.osgi.service.component", "version=\"1.3\"", "uses:=\"org.osgi.framework\"");
		assertPackage("org.osgi.service.component.runtime", "version=\"1.3\"", "uses:=\"org.osgi.framework,org.osgi.service.component.runtime.dto,org.osgi.util.promise\"");
		assertPackage("org.osgi.service.component.runtime.dto", "version=\"1.3\"", "uses:=\"org.osgi.dto,org.osgi.framework.dto\"");
		assertPackage("org.osgi.util.function", "version=\"1.0\"");
		assertPackage("org.osgi.util.promise", "version=\"1.0\"", "uses:=\"org.osgi.util.function\"");
		
		packagesWithMetaData.clear();
		String example3 = "de.odysseus.el,de.odysseus.el.util";
		ExportPackageUtil.parse(example3, collectingConsumer);
		Assert.assertEquals(2, packagesWithMetaData.size());
		assertPackage("de.odysseus.el");
		assertPackage("de.odysseus.el.util");
	}
	
	private void assertPackage(String packageName, String... metaData) {
		Map<String, List<String>> packagesWithMetaData = collectingConsumer.packagesWithMetaData;
		List<String> metaDataList = packagesWithMetaData.get(packageName);
		Assert.assertNotNull(metaDataList);
		Assert.assertEquals(metaData.length, metaDataList.size());
		for (int i = 0; i < metaData.length; i++) {
			Assert.assertEquals(metaData[i], metaDataList.get(i));
		}
		
	}
	
	@Test
	public void testVersionPattern() {
		Assert.assertEquals("3.3.0.Final", ExportPackageUtil.getVersionFromMetaData(Arrays.asList("version=\"3.3.0.Final\"")));
	}

}
