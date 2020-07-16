package org.bndly.schema.impl.repository;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.Path;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.PathBuilder;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.repository.ModificationNotAllowedException;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.services.Engine;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NodeImpl extends AbstractRepositoryItem implements Node, IndexManager.IndexContext {
	private final NodeImpl parent;
	private final String nodeName;
	private final String type;
	private final Path path;
	private final IndexManager nodeIndexManager;
	private final IndexManager propertyIndexManager;
	private final SortedKeyedIndex<String, NodeImpl> children;
	private final SortedKeyedIndex<String, PropertyImpl> properties;
	
	private long index;

	public static NodeImpl createRootNode(RecordContext ctx, RepositorySessionImpl repository, Engine engine) {
		return new NodeImpl(NodeTypes.ROOT, PathBuilder.newInstance().build(), ctx, repository, engine);
	}
	
	private NodeImpl(String type, Path path, RecordContext ctx, RepositorySessionImpl repository, Engine engine) {
		super(repository, engine, ctx);
		this.parent = null;
		this.path = path;
		List<String> elementNames = path.getElementNames();
		this.nodeName = elementNames.isEmpty() ? "" : elementNames.get(elementNames.size() - 1);
		if (type == null) {
			throw new IllegalArgumentException("type is not allowed to be null");
		}
		this.type = type;
		this.nodeIndexManager = new IndexManager(this);
		this.propertyIndexManager = new IndexManager(createPropertyIndexManagingItem());
		this.index = 0;
		this.children = createChildrenIndex();
		this.properties = createPropertiesIndex();
	}
	
	public NodeImpl(NodeImpl parent, String type, Path path, RecordContext ctx, RepositorySessionImpl repository, Record record, Engine engine) {
		super(repository, record, engine, ctx);
		this.parent = parent;
		List<String> elementNames = path.getElementNames();
		this.nodeName = elementNames.isEmpty() ? "" : elementNames.get(elementNames.size() - 1);
		this.path = path;
		if (type == null) {
			throw new IllegalArgumentException("type is not allowed to be null");
		}
		this.type = type;
		this.nodeIndexManager = new IndexManager(this);
		this.propertyIndexManager = new IndexManager(createPropertyIndexManagingItem());
		this.index = record.getAttributeValue("parentIndex", Long.class);
		this.children = createChildrenIndex();
		this.properties = createPropertiesIndex();
	}

	private SortedKeyedIndex<String, NodeImpl> createChildrenIndex() {
		final NodeImpl that = this;
		return new SortedKeyedIndex<String, NodeImpl>() {
			@Override
			protected boolean isTransient() {
				return that.isTransient();
			}

			@Override
			protected boolean isRemovalOfItemScheduled(NodeImpl item) {
				return item.isRemovalScheduled();
			}

			@Override
			protected Iterator<Record> performItemsQuery() {
				return getAccessor().query("PICK Node n IF n.parent.id=? ORDERBY n.parentIndex", getRecordContext(), null, getRecord() == null ? null : getRecord().getId());
			}

			@Override
			protected Iterator<Record> performItemQueryByKey(String key) {
				return getAccessor().query("PICK Node n IF n.name=? AND n.parent.id=? LIMIT ?", getRecordContext(), null, key, getRecord() == null ? null : getRecord().getId(), 1);
			}

			@Override
			protected NodeImpl wrapItemRecord(Record record) {
				final String name = record.getAttributeValue("name", String.class);
				Path childPath = PathBuilder.newInstance(path).element(name).build();
				NodeImpl current = createNodeInstance(that, childPath, record);
				return current;
			}

			@Override
			protected String getKeyOfItem(NodeImpl item) {
				return item.getName();
			}

			@Override
			protected NodeImpl setIndexOfItem(NodeImpl item, long index) throws RepositoryException {
				item.setIndex(index);
				return item;
			}

			@Override
			protected void throwItemNotFoundException(String keyOfItem) throws RepositoryException {
				throw new NodeNotFoundException("could not find node at " + path.toString() + "/" + keyOfItem);
			}

			@Override
			protected void testMovePreconditions(NodeImpl item, long index) throws RepositoryException {
				if (isReadOnly()) {
					throw new ModificationNotAllowedException("nodex can not be moved in read only sessions");
				}
				if (item.getParent() != that) {
					throw new RepositoryException("provided node is not owned by this node");
				}
			}

			@Override
			protected Long countItemsInPersistenceLayer() {
				return getAccessor().count("COUNT Node n IF n.parent.id=?", getRecord() == null ? null : getRecord().getId());
			}
			
		};
	}
	
	private SortedKeyedIndex<String, PropertyImpl> createPropertiesIndex() {
		final NodeImpl that = this;
		return new SortedKeyedIndex<String, PropertyImpl>() {
			@Override
			protected boolean isTransient() {
				return that.isTransient();
			}

			@Override
			protected boolean isRemovalOfItemScheduled(PropertyImpl item) {
				return item.isRemovalScheduled();
			}

			@Override
			protected Iterator<Record> performItemsQuery() {
				return getAccessor().query("PICK Property p IF p.node.id=? ORDERBY p.parentIndex", getRecordContext(), null, getRecord() == null ? null : getRecord().getId());
			}

			@Override
			protected Iterator<Record> performItemQueryByKey(String key) {
				return getAccessor().query("PICK Property p IF p.node.id=? AND p.name=? LIMIT ?", getRecordContext(), null, getRecord() == null ? null : getRecord().getId(), key, 1);
			}

			@Override
			protected PropertyImpl wrapItemRecord(Record record) {
				return createPropertyInstance(record);
			}

			@Override
			protected String getKeyOfItem(PropertyImpl item) {
				return item.getName();
			}

			@Override
			protected PropertyImpl setIndexOfItem(PropertyImpl item, long index) throws RepositoryException {
				item.setIndex(index);
				return item;
			}

			@Override
			protected void testMovePreconditions(PropertyImpl item, long index) throws RepositoryException {
				if (isReadOnly()) {
					throw new ModificationNotAllowedException("properties can not be moved in read only sessions");
				}
				if (item.getNode() != that) {
					throw new RepositoryException("provided property is not owned by this node");
				}
			}

			@Override
			protected void throwItemNotFoundException(String keyOfItem) throws RepositoryException {
				throw new PropertyNotFoundException("could not find property " + keyOfItem + " at node " + getPath().toString());
			}

			@Override
			protected Long countItemsInPersistenceLayer() {
				return getAccessor().count("COUNT Property p IF p.node.id=?", getRecord() == null ? null : getRecord().getId());
			}
			
		};
	}
	
	private IndexManager.IndexContext createPropertyIndexManagingItem() {
		final NodeImpl that = this;
		return new IndexManager.IndexContext() {
			@Override
			public boolean isTransient() {
				return that.isTransient();
			}

			@Override
			public long countChildren() throws RepositoryException {
				return getPropertyCount();
			}
		};
	}
	
	@Override
	public NodeImpl getParent() throws RepositoryException {
		return parent;
	}

	@Override
	public String getName() {
		return nodeName;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public NodeImpl createChild(String name, String type) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("nodes can not be created in read only sessions");
		}
		name = PathBuilder.filterUnallowedIndentifierChars(name);
		children.testKeyUniqueness(name);
		final Record childNodeRecord = getRecordContext().create("Node");
		if (getRecord() != null) {
			childNodeRecord.setAttributeValue("parent", getRecord());
		}
		childNodeRecord.setAttributeValue("name", name);
		childNodeRecord.setAttributeValue("nodeType", type);
		childNodeRecord.setAttributeValue("parentIndex", nodeIndexManager.pullNextChildIndex());
		Path childPath = PathBuilder.newInstance(getPath()).element(name).build();
		NodeImpl childNode = createNodeInstance(this, childPath, childNodeRecord);
		children.retain(childNode);
		childNode.createPersist(this);
		getRepositoryListenersLock().readLock().lock();
		try {
			for (RepositoryListener repositoryListener : getRepositoryListeners()) {
				repositoryListener.onNodeCreated(childNode);
			}
		} finally {
			getRepositoryListenersLock().readLock().unlock();
		}
		return childNode;
	}

	@Override
	public long countChildren() throws RepositoryException {
		return children.countItemsInPersistenceLayer();
	}
	
	private NodeImpl createNodeInstance(final NodeImpl parent, final Path path, final Record nodeRecord) {
		final String type = nodeRecord.getAttributeValue("nodeType", String.class);
		return new NodeImpl(parent, type, path, getRecordContext(), super.getRepositorySession(), nodeRecord, getEngine());
	}

	@Override
	public Iterator<Node> getChildren() throws RepositoryException {
		return (Iterator) children.getItems();
	}

	@Override
	public Node getChild(String name) throws RepositoryException {
		return children.getItem(name);
	}

	@Override
	public Iterator<Property> getProperties() throws RepositoryException {
		return (Iterator) properties.getItems();
	}

	@Override
	public Property getProperty(String name) throws RepositoryException {
		return properties.getItem(name);
	}

	@Override
	public boolean isHavingProperty(String name) throws RepositoryException {
		try {
			Property prop = getProperty(name);
			return prop != null;
		} catch (PropertyNotFoundException e) {
			// we catch this internally, because isHavingProperty is a convenience method
			return false;
		}
	}

	@Override
	public Property createProperty(String name, Property.Type type) throws RepositoryException {
		return createPropertyInternal(name, type, false);
	}

	@Override
	public Property createMultiProperty(String name, Property.Type type) throws RepositoryException {
		return createPropertyInternal(name, type, true);
	}
	
	private Property createPropertyInternal(String name, Property.Type type, boolean multiValue) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("properties can not be created in read only sessions");
		}
		name = PathBuilder.filterUnallowedIndentifierChars(name);
		properties.testNewItemInMemoryUniqueness(name);
		if (type == null) {
			throw new RepositoryException("type is not allowed to be null");
		}
		final Record propertyRecord = getRecordContext().create("Property");
		propertyRecord.setAttributeValue("type", type.toString());
		propertyRecord.setAttributeValue("name", name);
		propertyRecord.setAttributeValue("parentIndex", propertyIndexManager.pullNextChildIndex());
		propertyRecord.setAttributeValue("isMultiValued", multiValue);
		propertyRecord.setAttributeValue("node", getRecord());
		PropertyImpl property = createPropertyInstance(propertyRecord);
		properties.retain(property);
		property.createPersist(this);
		getRepositoryListenersLock().readLock().lock();
		try {
			for (RepositoryListener repositoryListener : getRepositoryListeners()) {
				repositoryListener.onPropertyCreated(property);
			}
		} finally {
			getRepositoryListenersLock().readLock().unlock();
		}
		return property;
	}

	/**
	 * Creates an instance of PropertyImpl. The instance will not be put to any list or map. It is just a convenience method for creating an object instance.
	 * @param propertyRecord the record, that holds the property data
	 * @return a fresh instance of PropertyImpl
	 */
	private PropertyImpl createPropertyInstance(final Record propertyRecord) {
		final String name = propertyRecord.getAttributeValue("name", String.class);
		final Property.Type propertyType = Property.Type.valueOf(propertyRecord.getAttributeValue("type", String.class));
		Boolean tmp = propertyRecord.getAttributeValue("isMultiValued", Boolean.class);
		final boolean multiValued = tmp == null ? false : tmp;
		return new PropertyImpl(propertyType, name, this, multiValued, super.getRepositorySession(), propertyRecord, getEngine(), getRecordContext());
	}

	@Override
	public void remove() throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("nodes can not be removed in read only sessions");
		}
		Record rec = getRecord();
		if (rec == null) {
			throw new RepositoryException("root node can not be removed");
		}
		createRemovable(parent);
		getRepositoryListenersLock().readLock().lock();
		try {
			for (RepositoryListener repositoryListener : getRepositoryListeners()) {
				repositoryListener.onNodeRemoved(this);
			}
		} finally {
			getRepositoryListenersLock().readLock().unlock();
		}
	}

	@Override
	protected void afterRemove() {
		if (parent == null) {
			return;
		}
		parent.children.drop(this);
	}

	public void dropProperty(PropertyImpl property) {
		properties.drop(property);
	}
	
	public long getPropertyCount() {
		return properties.getItemCount();
	}
	
	public void moveNodeToIndex(NodeImpl node, long index) throws RepositoryException {
		children.moveItemToIndex(node, index);
	}

	@Override
	public void moveToIndex(long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("nodes can not be moved in read only sessions");
		}
		if (getRecord() == null) {
			throw new ModificationNotAllowedException("root node can not be moved");
		}
		getParent().moveNodeToIndex(this, index);
		getRepositoryListenersLock().readLock().lock();
		try {
			for (RepositoryListener repositoryListener : getRepositoryListeners()) {
				repositoryListener.onNodeMoved(this, index);
			}
		} finally {
			getRepositoryListenersLock().readLock().unlock();
		}
	}

	public void movePropertyToIndex(PropertyImpl property, long index) throws RepositoryException {
		properties.moveItemToIndex(property, index);
	}
	
	@Override
	public long getIndex() {
		return this.index;
	}
	
	public void setIndex(long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("properties can not be moved in read only sessions");
		}
		if (getRecord() == null) {
			throw new ModificationNotAllowedException("the root node has no index");
		}
		if (index != this.index) {
			this.index = index;
			getRecord().setAttributeValue("parentIndex", index);
			if (!isTransient()) {
				createPersist(getParent());
			}
		}
	}

}
