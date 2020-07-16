package org.bndly.pdf.visualobject;

/*-
 * #%L
 * PDF Document Printer
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

import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import java.util.ArrayList;
import java.util.List;

import org.bndly.pdf.layout.Layout;

public class Container extends VisualObject {
	private Layout layout;
	private List<VisualObject> items;

	public Container(PrintingContext context, PrintingObject owner) {
		super(context, owner);
	}

//	public <T extends VisualObject> T createContained(Class<T> type) {
//		T instance = create(type);
//		add(instance);
//		return instance;
//	}
//	
//	public <T extends VisualObject> T createContainedAfter(Class<T> type, VisualObject visualObject) {
//		T instance = create(type);
//		insertAfter(instance, visualObject);
//		return instance;
//	}
	
	@SuppressWarnings("unchecked")
	public <T extends VisualObject> T getItem(Class<T> type) {
		List<T> tmp = getItems(type);
		if (tmp != null && tmp.size() == 1) {
			return tmp.get(0);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends VisualObject> List<T> getItems(Class<T> type) {
		if (items != null) {
			List<T> l = new ArrayList<>();
			for (VisualObject t : items) {
				if (type.isAssignableFrom(t.getClass())) {
					l.add((T) t);
				}
			}
			if (l.size() > 0) {
				return l;
			}
		}
		return null;
	}
	
	public List<VisualObject> getItems() {
		if (items != null) {
			return new ArrayList<>(items);
		}
		return items;
	}
	
	public void remove(VisualObject object) {
		if (items != null) {
			items.remove(object);
		}
	}
	
	public void add(VisualObject object) {
		insertAfter(object, null);
	}
	
	public void insertAfter(VisualObject object, VisualObject after) {
		Container oldOwner = object.getOwnerContainer();
		if (oldOwner != null) {
			oldOwner.remove(object);
		}
		if (items == null) {
			items = new ArrayList<>();
		}
		if (after != null) {
			int i = items.indexOf(after);
			if (i == -1) {
				items.add(object);
			}
			items.add(i + 1, object);
		} else {
			items.add(object);
		}
		object.ownerContainer = this;
	}

	public final Layout getLayout() {
		return layout;
	}

	public final <T extends Layout> T setLayout(T layoutInstance) {
		this.layout = layoutInstance;
		return layoutInstance;
	}
	
	public void doLayout() {
		if (layout == null) {
			layout = setLayout(getContext().createVerticalLayout(this));
		}
		layout.render();
	}
	
	@Override
	public Container copyTo(VisualObject owner) {
		Container copyContainer = getContext().createContainer(owner);
		List<VisualObject> subs = getItems();
		if (subs != null) {
			for (VisualObject sub : subs) {
				sub.copyTo(copyContainer);
			}
		}
		return copyContainer;
	}
	
//	public VisualObject copyToWithoutChildrenAs(VisualObject owner) {
//		Container copyContainer = getContext().createContainer(owner);
//		return copyContainer;
//	}

	public VisualObject copyToWithoutChildren(VisualObject owner) {
		Container copyContainer = getContext().createContainer(owner);
		return copyStyleClassesTo(copyContainer);
	}
	
	@Override
	protected String asStringIndented(int indent) {
		StringBuffer sb = new StringBuffer();
		String layoutString = "";
		if (layout != null) {
			layoutString = " : " + layout.getClass().getSimpleName();
		}
		sb.append(super.asStringIndented(indent)).append(layoutString);
		if (items != null) {
			for (VisualObject o : items) {
				sb.append("\n");
				sb.append(o.asStringIndented(indent + 1));
			}
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return asStringIndented(0);
	}

	public boolean isParentOf(VisualObject subObject) {
		boolean result = false;
		if ((items != null && items.contains(subObject)) || this == subObject) {
			return true;
		} else {
			List<Container> subContainers = getItems(Container.class);
			if (subContainers != null) {
				for (Container container : subContainers) {
					result = result || container.isParentOf(subObject);
				}
			}
		}

		return result;
	}
}
