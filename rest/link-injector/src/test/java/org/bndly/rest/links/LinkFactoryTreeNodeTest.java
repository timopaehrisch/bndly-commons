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

import org.bndly.rest.atomlink.impl.LinkFactoryTreeNode;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class LinkFactoryTreeNodeTest {

	/*
	Demo hierarchy:
              +---+                    +-----+   +-------+
              |Foo|                    |Bingo|   |Blazing|
              +---+                    +-----+   +-------+
                ^
  +-----------------------------+
  |             |               |
  |             |               |
+---+         +---+         +-------+
|Bar|         |Baz|         |Bambino|
+---+         +---+         +-------+
  ^
  |
  |
+--------+
|Blooming|
+--------+

	 */

	public static class Foo {}; 
	public static class Bar extends Foo {}; 
	public static class Baz extends Foo {};
	public static class Bingo {}; 
	public static class Blazing {}; 
	public static class Bambino extends Foo {}; 
	public static class Blooming extends Bar {}; 
	
	@Test
	public void testTreeCreation() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		Assert.assertTrue(root == root.getRoot(), "root was not root");
		LinkFactoryTreeNode fooNode = root.getOrCreateChildForType(Foo.class);
		LinkFactoryTreeNode barNode = fooNode.getOrCreateChildForType(Bar.class);
		LinkFactoryTreeNode bazNode = fooNode.getOrCreateChildForType(Baz.class);
		LinkFactoryTreeNode bingoNode = root.getOrCreateChildForType(Bingo.class);
		Assert.assertNull(root.getParent());
		Assert.assertTrue(fooNode.getParent() == root);
		Assert.assertTrue(barNode.getParent() == fooNode);
		Assert.assertTrue(bazNode.getParent() == fooNode);
		Assert.assertTrue(bingoNode.getParent() == root);
	}
	
	@Test
	public void testTreeCreationViaRoot() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		Assert.assertTrue(root == root.getRoot(), "root was not root");
		LinkFactoryTreeNode fooNode = root.getOrCreateChildForType(Foo.class);
		LinkFactoryTreeNode barNode = root.getOrCreateChildForType(Bar.class);
		LinkFactoryTreeNode bazNode = root.getOrCreateChildForType(Baz.class);
		LinkFactoryTreeNode bingoNode = root.getOrCreateChildForType(Bingo.class);
		Assert.assertNull(root.getParent());
		Assert.assertTrue(fooNode.getParent() == root);
		Assert.assertTrue(barNode.getParent() == fooNode);
		Assert.assertTrue(bazNode.getParent() == fooNode);
		Assert.assertTrue(bingoNode.getParent() == root);
	}
	
	@Test
	public void testTreeCreationSubtypeInsertedBeforParentType() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		LinkFactoryTreeNode bloomingNode = root.getOrCreateTypeHierarchy(Blooming.class);
		LinkFactoryTreeNode barNode = root.getOrCreateTypeHierarchy(Bar.class);
		LinkFactoryTreeNode fooNode = root.getOrCreateTypeHierarchy(Foo.class);
		Assert.assertTrue(fooNode.getParent() == root);
		Assert.assertTrue(barNode.getParent() == fooNode);
		Assert.assertTrue(bloomingNode.getParent() == barNode);
		
		root.checkAutoRemoval();
	}
	
	@Test
	public void testTreeCreationWithTypeHierarchy() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		LinkFactoryTreeNode bloomingNode = root.getOrCreateTypeHierarchy(Blooming.class);
		LinkFactoryTreeNode fooNode = root.findNodeForType(Foo.class);
		LinkFactoryTreeNode barNode = root.findNodeForType(Bar.class);
		Assert.assertTrue(fooNode.getParent() == root);
		Assert.assertTrue(barNode.getParent() == fooNode);
		Assert.assertTrue(bloomingNode.getParent() == barNode);
	}
	
	@Test
	public void testTreeLookup() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		LinkFactoryTreeNode fooNode = root.getOrCreateChildForType(Foo.class);
		LinkFactoryTreeNode barNode = fooNode.getOrCreateChildForType(Bar.class);
		LinkFactoryTreeNode bazNode = fooNode.getOrCreateChildForType(Baz.class);
		LinkFactoryTreeNode bingoNode = root.getOrCreateChildForType(Bingo.class);
		
		// lookup non-existing
		
		LinkFactoryTreeNode shouldBeRoot = root.findParentNodeForType(Blazing.class);
		Assert.assertTrue(shouldBeRoot == root);
		
		shouldBeRoot = fooNode.findParentNodeForType(Blazing.class);
		Assert.assertTrue(shouldBeRoot == root);
		
		shouldBeRoot = barNode.findParentNodeForType(Blazing.class);
		Assert.assertTrue(shouldBeRoot == root);
		
		// lookup existing
		
		LinkFactoryTreeNode shouldBeFoo = root.findParentNodeForType(Baz.class);
		Assert.assertTrue(shouldBeFoo == fooNode);
		
		shouldBeFoo = fooNode.findParentNodeForType(Baz.class);
		Assert.assertTrue(shouldBeFoo == fooNode);
		
		shouldBeFoo = bazNode.findParentNodeForType(Baz.class);
		Assert.assertTrue(shouldBeFoo == fooNode);
	}
	
	@Test
	public void testAutoRemoval() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		Assert.assertTrue(root == root.getRoot(), "root was not root");
		LinkFactoryTreeNode fooNode = root.getOrCreateChildForType(Foo.class);
		LinkFactoryTreeNode barNode = fooNode.getOrCreateChildForType(Bar.class);
		LinkFactoryTreeNode bazNode = fooNode.getOrCreateChildForType(Baz.class);
		LinkFactoryTreeNode bingoNode = root.getOrCreateChildForType(Bingo.class);
		
		fooNode.checkAutoRemoval();
		// now foo should be removed and bar and baz should be appended to foo's parent
		
		Assert.assertNull(root.findNodeForType(Foo.class));
		Assert.assertTrue(root.findParentNodeForType(Bar.class) == root);
		Assert.assertTrue(root.findParentNodeForType(Baz.class) == root);
		Assert.assertTrue(root.findParentNodeForType(Bingo.class) == root);
		Assert.assertTrue(root.findNodeForType(Bar.class) == barNode);
		Assert.assertTrue(root.findNodeForType(Baz.class) == bazNode);
		Assert.assertTrue(root.findNodeForType(Bingo.class) == bingoNode);
	}
	
	@Test
	public void testResort() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		Assert.assertTrue(root == root.getRoot(), "root was not root");
		LinkFactoryTreeNode fooNode = root.getOrCreateChildForType(Foo.class);
		LinkFactoryTreeNode barNode = root.getOrCreateChildForType(Bar.class);
		LinkFactoryTreeNode bazNode = root.getOrCreateChildForType(Baz.class);
		LinkFactoryTreeNode bingoNode = root.getOrCreateChildForType(Bingo.class);
		
		fooNode.resortSibblings();
		// now foo should be parent of bar and baz
		
		Assert.assertTrue(root.findParentNodeForType(Foo.class) == root);
		Assert.assertTrue(root.findParentNodeForType(Bar.class) == fooNode);
		Assert.assertTrue(root.findParentNodeForType(Baz.class) == fooNode);
		Assert.assertTrue(root.findParentNodeForType(Bingo.class) == root);
	}

	@Test
	public void testAllowSubclassesExtension() {
		LinkFactoryTreeNode root = new LinkFactoryTreeNode(Object.class);
		Assert.assertTrue(root == root.getRoot(), "root was not root");
		LinkFactoryTreeNode fooNode = root.getOrCreateChildForType(Foo.class);
		LinkFactoryTreeNode barNode = root.getOrCreateChildForType(Bar.class);

		fooNode.resortSibblings();
		// now foo should be parent of bar and baz

		Assert.assertTrue(root.findParentNodeForType(Foo.class) == root);
		Assert.assertTrue(root.findParentNodeForType(Bar.class) == fooNode);
		Assert.assertTrue(root.findParentNodeForType(Blooming.class) == barNode);
		Assert.assertTrue(root.findParentNodeForType(Bingo.class) == root);

		Assert.assertTrue(root.findNodeForType(Blooming.class) == null); // there is no direct node for Blooming
		Assert.assertEquals(root.findClosestNodeForType(Blooming.class), barNode); // because Blooming extends Bar
		Assert.assertEquals(root.findNodeForType(Bar.class), barNode);
		Assert.assertEquals(root.findNodeForType(Foo.class), fooNode);
	}
}
