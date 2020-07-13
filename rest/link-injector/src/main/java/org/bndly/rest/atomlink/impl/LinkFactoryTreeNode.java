package org.bndly.rest.atomlink.impl;

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

import org.bndly.common.lang.FilteringIterator;
import org.bndly.common.lang.IteratorChain;
import org.bndly.common.lang.TransformingIterator;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.LinkFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class LinkFactoryTreeNode {

	private final Class targetType;
	private LinkFactoryTreeNode parent;
	private final Map<Class, LinkFactoryTreeNode> childrenByTargetType = new HashMap<>();
	private final List<LinkFactory> linkFactories = new ArrayList<>();
	private LinkSetter linkSetter;

	public LinkFactoryTreeNode(Class targetType) {
		this.targetType = targetType;
	}

	public LinkFactoryTreeNode getParent() {
		return parent;
	}

	public LinkSetter getLocalLinkSetter() {
		return linkSetter;
	}
	
	public LinkSetter getLinkSetter() {
		LinkFactoryTreeNode node = this;
		while (node != null) {
			if (node.linkSetter != null) {
				return node.linkSetter;
			}
			node = node.parent;
		}
		return null;
	}

	public void setLinkSetter(LinkSetter linkSetter) {
		this.linkSetter = linkSetter;
	}

	public Iterator<LinkFactory> getLinkFactories() {
		return linkFactories.iterator();
	}

	public LinkFactoryTreeNode getOrCreateTypeHierarchy(Class childTargetType) {
		LinkFactoryTreeNode root = getRoot();
		return _getOrCreateTypeHierarchy(childTargetType, root);
	}
	
	private LinkFactoryTreeNode _getOrCreateTypeHierarchy(Class childTargetType, LinkFactoryTreeNode parent) {
		Class childsParentType = childTargetType.getSuperclass();
		if (childsParentType != null && !Object.class.equals(childsParentType)) {
			parent = _getOrCreateTypeHierarchy(childsParentType, parent);
		}
		LinkFactoryTreeNode child = parent.childrenByTargetType.get(childTargetType);
		if (child == null) {
			child = new LinkFactoryTreeNode(childTargetType);
			child.parent = parent;
			parent.childrenByTargetType.put(childTargetType, child);
		}
		return child;
	}
	
	public LinkFactoryTreeNode getOrCreateChildForType(Class childTargetType) {
		LinkFactoryTreeNode appendNodeHere = findParentNodeForType(childTargetType);
		if (appendNodeHere == null || !appendNodeHere.targetType.isAssignableFrom(childTargetType)) {
			throw new IllegalArgumentException("unallowed child type.");
		}
		LinkFactoryTreeNode child = appendNodeHere.childrenByTargetType.get(childTargetType);
		if (child == null) {
			child = new LinkFactoryTreeNode(childTargetType);
			child.parent = appendNodeHere;
			appendNodeHere.childrenByTargetType.put(childTargetType, child);
		}
		return child;
	}

	public void addLinkFactory(LinkFactory linkFactory) {
		if (linkFactory != null) {
			linkFactories.add(linkFactory);
		}
	}

	public void removeLinkFactory(LinkFactory linkFactory) {
		if (linkFactory != null) {
			Iterator<LinkFactory> it = linkFactories.iterator();
			while (it.hasNext()) {
				LinkFactory next = it.next();
				if (next == linkFactory) {
					it.remove();
				}
			}
		}
	}

	public void cleanUp() {
		getRoot().cleanUpChildren();
	}
	
	public void cleanUpChildren() {
		for (LinkFactoryTreeNode child : childrenByTargetType.values()) {
			if (!child.isUseful()) {
				childrenByTargetType.remove(child.targetType);
				child.parent = null;
			}
		}
		if (!isUseful() && parent != null) {
			 parent.childrenByTargetType.remove(targetType);
			 parent = null;
		}
	}
	
	public boolean isUseful() {
		if (!linkFactories.isEmpty() || parent == null) {
			return true;
		}
		for (LinkFactoryTreeNode child : childrenByTargetType.values()) {
			if (child.isUseful()) {
				return true;
			}
		}
		return false;
	}
	
	public void checkAutoRemoval() {
		if (linkFactories.isEmpty() && parent != null) {
			// auto removal. this tree node is worthless.

			// remove self as a child from the parent.
			// no the sub tree is detached.
			parent.childrenByTargetType.remove(this.targetType);

			LinkFactoryTreeNode root = getRoot();
			// children might still have their purpose. hence they will be re-appended to the tree.
			for (Map.Entry<Class, LinkFactoryTreeNode> entrySet : childrenByTargetType.entrySet()) {
				Class key = entrySet.getKey();
				LinkFactoryTreeNode value = entrySet.getValue();
				LinkFactoryTreeNode newParent = findParentNodeForTypeInTreeNode(key, root);
				if (newParent != null) {
					value.parent = newParent;
					newParent.childrenByTargetType.put(key, value);
				}
			}
			childrenByTargetType.clear();
		}
	}
	
	public void resortSibblings() {
		if (parent == null) {
			return;
		}
		Iterator<LinkFactoryTreeNode> sibblings = parent.childrenByTargetType.values().iterator();
		while (sibblings.hasNext()) {
			LinkFactoryTreeNode sibbling = sibblings.next();
			if (sibbling == this) {
				continue;
			}
			if (targetType.isAssignableFrom(sibbling.targetType)) {
				// sibbling is in fact a child of this
				sibblings.remove();
				sibbling.parent = this;
				childrenByTargetType.put(sibbling.targetType, sibbling);
			}
		}
	}

	public final LinkFactoryTreeNode getRoot() {
		LinkFactoryTreeNode p = this;
		while (p.parent != null) {
			p = p.parent;
		}
		return p;
	}

	public final LinkFactoryTreeNode findClosestNodeForType(Class childType) {
		return findClosestNodeForType(childType, getRoot());
	}

	public final LinkFactoryTreeNode findClosestNodeForType(Class childType, LinkFactoryTreeNode treeNode) {
		if (treeNode.targetType.equals(childType)) {
			return treeNode;
		}

		if (!treeNode.targetType.isAssignableFrom(childType)) {
			// this would be a dead end
			return null;
		}
		for (Map.Entry<Class, LinkFactoryTreeNode> entry : treeNode.childrenByTargetType.entrySet()) {
			LinkFactoryTreeNode candidate = findClosestNodeForType(childType, entry.getValue());
			if (candidate != null) {
				return candidate;
			}
		}
		return treeNode;
	}

	public final LinkFactoryTreeNode findNodeForType(Class childType) {
		return findNodeForType(childType, getRoot());
	}

	public final LinkFactoryTreeNode findNodeForType(Class childType, LinkFactoryTreeNode treeNode) {
		LinkFactoryTreeNode node = findParentNodeForTypeInTreeNode(childType, treeNode, false);
		if (node.targetType.equals(childType)) {
			return node;
		} else {
			return null;
		}
	}
	
	public final LinkFactoryTreeNode findParentNodeForType(Class childType) {
		LinkFactoryTreeNode root = getRoot();
		return findParentNodeForTypeInTreeNode(childType, root);
	}

	public final LinkFactoryTreeNode findParentNodeForTypeInTreeNode(Class childType, LinkFactoryTreeNode treeNode) {
		return findParentNodeForTypeInTreeNode(childType, treeNode, true);
	}
	
	private LinkFactoryTreeNode findParentNodeForTypeInTreeNode(Class childType, LinkFactoryTreeNode treeNode, boolean stopAtParent) {
		if (!treeNode.targetType.isAssignableFrom(childType)) {
			return null;
		}
		LinkFactoryTreeNode current = treeNode;
		for (Map.Entry<Class, LinkFactoryTreeNode> entrySet : current.childrenByTargetType.entrySet()) {
			LinkFactoryTreeNode value = entrySet.getValue();
			if (value.targetType.isAssignableFrom(childType)) {
				if (stopAtParent && value.targetType.equals(childType)) {
					// we are already in the parent, because the type is equal.
					break;
				}
				current = value;
				break;
			}
		}
		if (current != treeNode) {
			current = findParentNodeForTypeInTreeNode(childType, current, stopAtParent);
		}
		return current;
	}

	void injectInto(Object entity, boolean isRoot) {
		boolean checkForAllowSubclasses = !targetType.equals(entity.getClass());
		LinkSetter ls = getLinkSetter();
		if (ls != null) {
			Iterator<LinkFactory> factories = getLinkFactories();
			while (factories.hasNext()) {
				LinkFactory factory = factories.next();
				if(checkForAllowSubclasses && !factory.isSupportingSubTypes()) {
					// skip
					continue;
				}
				Iterator<AtomLinkBean> linkIterator = factory.buildLinks(entity, isRoot);
				while (linkIterator.hasNext()) {
					AtomLinkBean link = linkIterator.next();
					// write link to entity
					ls.setLinkInto(link, entity);
				}
			}
		}
		LinkFactoryTreeNode current = this.getParent();;
		while(current != null) {
			Iterator<LinkFactory> factories = current.getLinkFactories();
			while (factories.hasNext()) {
				LinkFactory factory = factories.next();
				if(!factory.isSupportingSubTypes()) {
					// skip the factory. we have to explicitly support sub types
					continue;
				}
				Iterator<AtomLinkBean> linkIterator = factory.buildLinks(entity, isRoot);
				while (linkIterator.hasNext()) {
					AtomLinkBean link = linkIterator.next();
					// write link to entity
					ls.setLinkInto(link, entity);
				}
			}
			current = current.getParent();
		}
	}

	private static interface ClassNameTester {
		boolean test(Class type, String nameToTest);
	}
	
	private static final ClassNameTester SIMPLE_CLASS_NAME_TESTER = new ClassNameTester() {
		@Override
		public boolean test(Class type, String nameToTest) {
			return type.getSimpleName().equals(nameToTest);
		}
	};
	private static final ClassNameTester CANONICAL_CLASS_NAME_TESTER = new ClassNameTester() {
		@Override
		public boolean test(Class type, String nameToTest) {
			return type.getName().equals(nameToTest);
		}
	};

	public Iterator<LinkFactory> getLinkFactoriesForSimpleClassName(String targetClassName) {
		return getLinkFactoriesForClassName(targetClassName, SIMPLE_CLASS_NAME_TESTER);
	}
	public Iterator<LinkFactory> getLinkFactoriesForClassName(String targetClassName) {
		return getLinkFactoriesForClassName(targetClassName, CANONICAL_CLASS_NAME_TESTER);
	}
	
	private Iterator<LinkFactory> getLinkFactoriesForClassName(final String targetClassName, final ClassNameTester classNameTester) {
		Collection<LinkFactoryTreeNode> values = childrenByTargetType.values();
		if (values.isEmpty()) {
			// there are no children, hence we can test with the local class
			return new FilteringIterator<LinkFactory>(getLinkFactories()) {
				@Override
				protected boolean isAccepted(LinkFactory toCheck) {
					return classNameTester.test(toCheck.getTargetType(), targetClassName);
				}
			};
		} else {
			Iterator<Iterator<LinkFactory>> childIter = new TransformingIterator<Iterator<LinkFactory>, LinkFactoryTreeNode>(values.iterator()) {
				@Override
				protected Iterator<LinkFactory> transform(LinkFactoryTreeNode toTransform) {
					return toTransform.getLinkFactoriesForSimpleClassName(targetClassName);
				}
			};
			return new IteratorChain<LinkFactory>(new FilteringIterator<LinkFactory>(getLinkFactories()) {
				@Override
				protected boolean isAccepted(LinkFactory toCheck) {
					return classNameTester.test(toCheck.getTargetType(), targetClassName);
				}
			}, IteratorChain.fromIterators(childIter));
		}
	}

}
