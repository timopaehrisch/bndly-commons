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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.repository.IndexedItem;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a utility class to maintain sorted and keyed lists of things.
 * Typically those things are child nodes or properties of a node.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class SortedKeyedIndex<KEY,ITEM extends IndexedItem> {
	
	private boolean didLoadItems;
	private boolean sortingItemsRequired;
	private List<ITEM> itemList;
	private Map<KEY, ITEM> itemsByKey;
	
	protected abstract boolean isTransient();
	protected abstract Iterator<Record> performItemsQuery();
	protected abstract Iterator<Record> performItemQueryByKey(KEY key);
	protected abstract ITEM wrapItemRecord(Record record);
	protected abstract KEY getKeyOfItem(ITEM item);
	protected abstract ITEM setIndexOfItem(ITEM item, long index) throws RepositoryException;
	protected abstract boolean isRemovalOfItemScheduled(ITEM item);
	protected abstract void testMovePreconditions(ITEM item, long index) throws RepositoryException;
	protected abstract void throwItemNotFoundException(KEY keyOfItem) throws RepositoryException;
	protected abstract Long countItemsInPersistenceLayer();
	
	/**
	 * Add an item to the end of the index
	 * @param item the item to add
	 */
	public final void retain(ITEM item) {
		KEY key = getKeyOfItem(item);
		// if we retain a property, that is already present by a different instance, we drop the older instance
		if (itemsByKey != null && itemList != null) {
			ITEM found = itemsByKey.get(key);
			if (found != null) {
				Iterator<ITEM> iterator = itemList.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == found) {
						iterator.remove();
					}
				}
			}
		}
		
		if (itemList == null) {
			itemList = new ArrayList<>();
		}
		if (itemsByKey == null) {
			itemsByKey = new LinkedHashMap<>();
		}
		itemList.add(item);
		itemsByKey.put(key, item);
	}
	
	/**
	 * Remove an item from the index
	 * @param item the item to remove
	 */
	public final void drop(ITEM item) {
		if (itemList != null) {
			Iterator<ITEM> iterator = itemList.iterator();
			while (iterator.hasNext()) {
				if (iterator.next() == item) {
					iterator.remove();
					sortingItemsRequired = true;
				}
			}
		}
		if (itemsByKey != null) {
			KEY key = getKeyOfItem(item);
			ITEM found = itemsByKey.get(key);
			if (found == item) {
				itemsByKey.remove(key);
				sortingItemsRequired = true;
			}
		}
	}
	
	public final void moveItemToIndex(ITEM item, long index) throws RepositoryException {
		testMovePreconditions(item, index);
		if (item.getIndex() == index) {
			return;
		}
		if (!isTransient()) {
			loadItems();
		}
		sortItemsIfRequired();
		if (itemList != null) {
			Iterator<ITEM> iterator = itemList.iterator();
			if (index > itemList.size() - 1) {
				index = itemList.size();
			}
			long lowerBorder;
			long upperBorder;
			boolean moveToRight = item.getIndex() < index;
			if (moveToRight) {
				lowerBorder = item.getIndex();
				upperBorder = index;
			} else {
				lowerBorder = index;
				upperBorder = item.getIndex();
			}
			long i = 0;
			while (iterator.hasNext()) {
				ITEM next = iterator.next();
				if (i >= lowerBorder && i <= upperBorder) {
					if (moveToRight) {
						if (next == item) {
							setIndexOfItem(next, index);
							iterator.remove();
						} else {
							setIndexOfItem(next, next.getIndex() - 1);
						}
					} else {
						if (next == item) {
							setIndexOfItem(next, index);
							iterator.remove();
						} else {
							setIndexOfItem(next, next.getIndex() + 1);
						}
					}
				}
				i++;
			}
			itemList.add((int) index, item);
			sortItemsByKey();
		}
	}
	
	public final Iterator<ITEM> getItems() throws RepositoryException {
		if (!isTransient()) {
			loadItems();
		}
		sortItemsIfRequired();
		return itemList == null ? Collections.EMPTY_LIST.iterator() : itemList.iterator();
	}
	
	public final ITEM getItem(KEY key) throws RepositoryException {
		if (itemsByKey != null && itemsByKey.containsKey(key)) {
			return filterRemovedItem(itemsByKey.get(key));
		}
		if (isTransient()) {
			throwItemNotFoundException(key);
			throw new RepositoryException("could not find item for key " + key + " because the owner is in a transient state");
		}
		Iterator<Record> r = performItemQueryByKey(key);
		if (r.hasNext()) {
			Record rec = r.next();
			ITEM item = wrapItemRecord(rec);
			retain(item);
			return item;
		} else {
			throwItemNotFoundException(key);
			throw new RepositoryException("could not find item for key " + key);
		}
	}
	
	public final void testKeyUniqueness(KEY key) throws RepositoryException {
		if (itemsByKey != null && itemsByKey.containsKey(key)) {
			throw new RepositoryException("item with key " + key + " already exists.");
		}
	}
	
	public final void testNewItemInMemoryUniqueness(KEY key) throws RepositoryException {
		if (itemsByKey != null && itemsByKey.containsKey(key)) {
			ITEM tmp = itemsByKey.get(key);
			if (tmp != null) {
				if (!isRemovalOfItemScheduled(tmp)) {
					throw new RepositoryException("item with key " + key + " already exists.");
				} else {
					// then the item will be removed anyway. so we can create a new one without running into problems
				}
			}
		}
	}
	
	public final long getItemCount() {
		if (isTransient()) {
			return itemList == null ? 0 : itemList.size();
		} else {
			if (didLoadItems) {
				return itemList == null ? 0 : itemList.size();
			} else {
				Long count = countItemsInPersistenceLayer();
				if (count == null) {
					return 0;
				}
				return count;
			}
		}
	}
	
	private ITEM filterRemovedItem(ITEM item) throws RepositoryException {
		if (isRemovalOfItemScheduled(item)) {
			throwItemNotFoundException(getKeyOfItem(item));
		}
		return item;
	}
	
	private void sortItemsIfRequired() {
		if (sortingItemsRequired) {
			if (itemList != null) {
				Collections.sort(itemList, IndexedItemComparator.INSTANCE);
				sortItemsByKey();
			}
			sortingItemsRequired = false;
		}
	}
	
	private void sortItemsByKey() {
		if (itemsByKey == null) {
			return;
		}
		itemsByKey.clear();
		for (ITEM item : itemList) {
			KEY key = getKeyOfItem(item);
			itemsByKey.put(key, item);
		}
	}
	
	private void loadItems() {
		if (didLoadItems) {
			return;
		}
		if (isTransient()) {
			return;
		}
		final Iterator<Record> itemsIter = performItemsQuery();
		didLoadItems = true;
		while (itemsIter.hasNext()) {
			Record record = itemsIter.next();
			ITEM item = wrapItemRecord(record);
			KEY key = getKeyOfItem(item);
			if (itemsByKey == null || !itemsByKey.containsKey(key)) {
				retain(item);
			} else {
				sortingItemsRequired = true;
			}
		}
	}


}
