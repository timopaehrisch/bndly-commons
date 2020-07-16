package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import org.bndly.common.app.PackageHeaderParser.PackageDescription;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PackageHeaderParserTest implements Consumer<PackageDescription>{

	private final List<PackageDescription> descs = new ArrayList<>();
	private final Set<String> packageNames = new HashSet<>();

	@BeforeMethod
	public void before() {
		descs.clear();
		packageNames.clear();
	}
	
	@Test
	public void testPackageParsingOfSinglePackageWithoutMetaData() {
		new PackageHeaderParser().parse("com.acme", this);
		assertEquals(packageNames.size(), 1);
		assertTrue(packageNames.contains("com.acme"));
	}
	
	@Test
	public void testPackageParsingOfMultiplePackagesWithoutMetaData() {
		new PackageHeaderParser().parse("com.acme,com.foo", this);
		assertEquals(packageNames.size(), 2);
		assertTrue(packageNames.contains("com.acme"));
		assertTrue(packageNames.contains("com.foo"));
	}
	
	@Test
	public void testPackageParsingOfMultiplePackagesWithMetaData() {
		new PackageHeaderParser().parse("org.bndly.java11.jaxb; version=\"3.0.5\"; uses:=\"org.osgi.framework\", org.bndly.java11.jaxb.x; version=\"3.0.5\"; uses:=\"javax.xml.bind.annotation\"", this);
		assertEquals(packageNames.size(), 2);
		assertTrue(packageNames.contains("org.bndly.java11.jaxb"));
		assertTrue(packageNames.contains("org.bndly.java11.jaxb.x"));
	}
	
	@Test
	public void testPackageParsingOfMultiplePackagesWithMetaDataAtStart() {
		new PackageHeaderParser().parse("org.bndly.java11.jaxb; version=\"3.0.5\"; uses:=\"org.osgi.framework\", org.bndly.java11.jaxb.x", this);
		assertEquals(packageNames.size(), 2);
		assertTrue(packageNames.contains("org.bndly.java11.jaxb"));
		assertTrue(packageNames.contains("org.bndly.java11.jaxb.x"));
	}
	
	@Test
	public void testPackageParsingOfMultiplePackagesWithMetaDataAtEnd() {
		new PackageHeaderParser().parse("org.bndly.java11.jaxb, org.bndly.java11.jaxb.x; version=\"3.0.5\"; uses:=\"javax.xml.bind.annotation\"", this);
		assertEquals(packageNames.size(), 2);
		assertTrue(packageNames.contains("org.bndly.java11.jaxb"));
		assertTrue(packageNames.contains("org.bndly.java11.jaxb.x"));
	}

	@Override
	public void accept(PackageDescription desc) {
		descs.add(desc);
		packageNames.add(desc.getName());
	}
}
