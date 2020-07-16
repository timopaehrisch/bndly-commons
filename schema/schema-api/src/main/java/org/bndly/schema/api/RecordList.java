package org.bndly.schema.api;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.api.RecordList.Listener;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.model.InverseAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RecordList implements List<Record> {
	public static interface Listener {
		Record beforeItemRemoved(Record record);
		void onItemRemoved(Record record);
		Record beforeItemAdded(Record record);
		void onItemAdded(Record record);
	}
	
	public static class NoOpListener implements Listener {

		@Override
		public void onItemRemoved(Record record) {}

		@Override
		public void onItemAdded(Record record) {}

		@Override
		public Record beforeItemAdded(Record record) { return record; }

		@Override
		public Record beforeItemRemoved(Record record) { return record; }
		
	}
	
	public static final Listener NOOP = new NoOpListener();
	private final Record owner;
	private final InverseAttribute inverseAttribute;
	private List<Record> original;
	private final Listener listener;
	private boolean didInitialize;

	public RecordList(Record owner, InverseAttribute inverseAttribute) {
		this(owner, inverseAttribute, NOOP);
	}
	
	public RecordList(Record owner, InverseAttribute inverseAttribute, Listener listener) {
		if (owner == null) {
			throw new IllegalArgumentException("can not create a record list without an owning record");
		}
		if (inverseAttribute == null) {
			throw new IllegalArgumentException("can not create a record list without an owning inverseAttribute");
		}
		if (listener == null) {
			throw new IllegalArgumentException("can not create a record list without a listener. consider using the noop listener or a different constructor.");
		}
		this.owner = owner;
		this.listener = listener;
		this.inverseAttribute = inverseAttribute;
	}
	
	public final RecordContext getContext() {
		return owner.getContext();
	}

	public final Record getOwner() {
		return owner;
	}

	public final InverseAttribute getInverseAttribute() {
		return inverseAttribute;
	}
	
	protected List<Record> initializeOriginal() {
		return new ArrayList<>();
	}

	public final boolean didInitialize() {
		return didInitialize;
	}
	
	private List<Record> getOriginal() {
		if (original == null) {
			original = initializeOriginal();
			didInitialize = true;
		}
		if (original == null) {
			throw new SchemaException("original record list was not correctly initialized");
		}
		return original;
	}
	
	@Override
	public int size() {
		return getOriginal().size();
	}

	@Override
	public boolean isEmpty() {
		return getOriginal().isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return getOriginal().contains(o);
	}

	@Override
	public Iterator<Record> iterator() {
		final RecordList parent = this;
		
		return new Iterator<Record>() {
			
			private Iterator<Record> wrappedIter;
			private Record current;
			
			private Iterator<Record> getIter() {
				if (wrappedIter == null) {
					wrappedIter = parent.getOriginal().iterator();
					if (wrappedIter == null) {
						throw new SchemaException("could not retrieve iterator from original record list");
					}
				}
				return wrappedIter;
			}
			
			@Override
			public boolean hasNext() {
				boolean r = getIter().hasNext();
				if (!r) {
					current = null;
				}
				return r;
			}

			@Override
			public Record next() {
				current = getIter().next();
				return current;
			}

			@Override
			public void remove() {
				if (current != null && listener != NOOP) {
					current = listener.beforeItemRemoved(current);
				}
				getIter().remove();
				if (current != null && listener != NOOP) {
					listener.onItemRemoved(current);
				}
			}

		};
	}

	@Override
	public Object[] toArray() {
		return getOriginal().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return getOriginal().toArray(a);
	}

	@Override
	public boolean add(Record e) {
		if (listener != NOOP) {
			e = listener.beforeItemAdded(e);
		}
		boolean res = getOriginal().add(e);
		if (listener != NOOP) {
			listener.onItemAdded(e);
		}
		return res;
	}

	@Override
	public boolean remove(Object o) {
		if (Record.class.isInstance(o) && listener != NOOP) {
			o = listener.beforeItemRemoved((Record) o);
		}
		boolean r = getOriginal().remove(o);
		if (listener != NOOP) {
			listener.onItemRemoved((Record) o);
		}
		return r;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return getOriginal().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Record> c) {
		if (listener != NOOP) {
			ArrayList<Record> defensiveCopy = new ArrayList<>();
			for (Record c1 : c) {
				defensiveCopy.add(listener.beforeItemAdded(c1));
			}
			c = defensiveCopy;
		}
		boolean res = getOriginal().addAll(c);
		if (listener != NOOP) {
			for (Record c1 : c) {
				listener.onItemAdded(c1);
			}
		}
		return res;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Record> c) {
		if (listener != NOOP) {
			ArrayList<Record> defensiveCopy = new ArrayList<>();
			for (Record c1 : c) {
				defensiveCopy.add(listener.beforeItemAdded(c1));
			}
			c = defensiveCopy;
		}
		boolean res = getOriginal().addAll(index, c);
		if (listener != NOOP) {
			for (Record c1 : c) {
				listener.onItemAdded(c1);
			}
		}
		return res;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (listener != NOOP) {
			ArrayList<Record> defensiveCopy = new ArrayList<>();
			for (Object removedItem : c) {
				if (Record.class.isInstance(removedItem)) {
					defensiveCopy.add(listener.beforeItemRemoved((Record) removedItem));
				}
			}
			c = defensiveCopy;
		}
		boolean r = getOriginal().removeAll(c);
		if (listener != NOOP) {
			for (Object removedItem : c) {
				if (Record.class.isInstance(removedItem)) {
					listener.onItemRemoved((Record) removedItem);
				}
			}
		}
		return r;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (listener != NOOP) {
			Iterator<Record> iter = iterator();
			boolean res = false;
			while (iter.hasNext()) {
				Record next = iter.next();
				for (Object c1 : c) {
					if (c1 == next) {
						res = true;
						iter.remove();
						break;
					}
				}
			}
			return res;
		} else {
			return getOriginal().retainAll(c);
		}
	}

	@Override
	public void clear() {
		if (listener != NOOP) {
			List<Record> defensiveCopy = new ArrayList<>();
			for (Record original1 : getOriginal()) {
				defensiveCopy.add(listener.beforeItemRemoved(original1));
			}
			getOriginal().clear();
			for (Record copy : defensiveCopy) {
				listener.onItemRemoved(copy);
			}
		} else {
			getOriginal().clear();
		}
	}

	@Override
	public Record get(int index) {
		return getOriginal().get(index);
	}

	@Override
	public Record set(int index, Record element) {
		if (listener != NOOP) {
			element = listener.beforeItemAdded(element);
		}
		Record r = getOriginal().set(index, element);
		if (listener != NOOP) {
			listener.onItemAdded(element);
		}
		return r;
	}

	@Override
	public void add(int index, Record element) {
		if (listener != NOOP) {
			element = listener.beforeItemAdded(element);
		}
		getOriginal().add(index, element);
		if (listener != NOOP) {
			listener.onItemAdded(element);
		}
	}

	@Override
	public Record remove(int index) {
		Record item;
		if (listener != NOOP) {
			item = get(index);
			item = listener.beforeItemRemoved(item);
			getOriginal().remove(index);
			listener.onItemRemoved(item);
		} else {
			item = getOriginal().remove(index);
		}
		return item;
	}

	@Override
	public int indexOf(Object o) {
		return getOriginal().indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return getOriginal().lastIndexOf(o);
	}

	@Override
	public ListIterator<Record> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<Record> listIterator(final int index) {
		final RecordList parent = this;
		
		return new ListIterator<Record>() {
			
			private ListIterator<Record> wrappedIter;
			private Record current;
			
			private ListIterator<Record> getIter() {
				if (wrappedIter == null) {
					wrappedIter = parent.getOriginal().listIterator(index);
					if (wrappedIter == null) {
						throw new SchemaException("could not retrieve iterator from original record list");
					}
				}
				return wrappedIter;
			}
			
			@Override
			public boolean hasNext() {
				boolean r = getIter().hasNext();
				if (!r) {
					current = null;
				}
				return r;
			}

			@Override
			public Record next() {
				current = getIter().next();
				return current;
			}

			@Override
			public boolean hasPrevious() {
				boolean r = getIter().hasPrevious();
				if (!r) {
					current = null;
				}
				return r;
			}

			@Override
			public Record previous() {
				current = getIter().previous();
				return current;
			}

			@Override
			public int nextIndex() {
				return getIter().nextIndex();
			}

			@Override
			public int previousIndex() {
				return getIter().previousIndex();
			}

			@Override
			public void remove() {
				if (current != null && listener != NOOP) {
					current = listener.beforeItemRemoved(current);
				}
				getIter().remove();
				if (current != null && listener != NOOP) {
					listener.onItemRemoved(current);
				}
			}

			@Override
			public void set(Record e) {
				// not actually added, but it is a new item, that appears in the list

				if (parent.listener != NOOP) {
					e = parent.listener.beforeItemAdded(e);
				}
				getIter().set(e);
				if (parent.listener != NOOP) {
					parent.listener.onItemAdded(e);
				}
			}

			@Override
			public void add(Record e) {
				if (parent.listener != NOOP) {
					e = parent.listener.beforeItemAdded(e);
				}
				getIter().add(e);
				if (parent.listener != NOOP) {
					parent.listener.onItemAdded(e);
				}
			}

		};
	}

	@Override
	public List<Record> subList(final int fromIndex, final int toIndex) {
		final RecordList parent = this;
		return new RecordList(owner, inverseAttribute, listener) {

			@Override
			protected List<Record> initializeOriginal() {
				return parent.getOriginal().subList(fromIndex, toIndex);
			}
			
		};
	}
	
}
