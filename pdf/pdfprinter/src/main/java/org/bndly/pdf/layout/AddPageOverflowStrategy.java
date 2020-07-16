package org.bndly.pdf.layout;

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
import java.util.ArrayList;
import java.util.List;

import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.PrintingObjectImpl;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.Document;
import org.bndly.pdf.visualobject.OverflowPage;
import org.bndly.pdf.visualobject.Page;
import org.bndly.pdf.visualobject.PageTemplate;
import org.bndly.pdf.visualobject.VisualObject;

public class AddPageOverflowStrategy extends PrintingObjectImpl implements VerticalOverflowStrategy {

	public AddPageOverflowStrategy(PrintingContext context, PrintingObject owner) {
		super(context, owner);
	}
	
	private Document getDocumentOf(VisualObject visualObject) {
		return getOwnerContainerByType(Document.class, visualObject);
	}
	
	private Page getPageOf(VisualObject visualObject) {
		return getOwnerContainerByType(Page.class, visualObject);
	}
	
	private <T extends Container> T getOwnerContainerByType(Class<T> type, VisualObject visualObject) {
		if (visualObject.is(type)) {
			return visualObject.as(type);
		}
		if (visualObject.getOwnerContainer() == null) {
			return null;
		}
		return getOwnerContainerByType(type, visualObject.getOwnerContainer());
	}
	
	@Override
	public void handleVerticalOverflowIn(Container container, VisualObject overflownObject, Double maxHeight, Double overflownObjectMaxHeight) {
		if (!container.isParentOf(overflownObject)) {
			return;
		}
		Document document = getDocumentOf(container);
		if (document != null) {
			Page page = getPageOf(container);
			if (page != null) {
				// clone the parent containers to the new page
				Container overflowToContainer = cloneParentContainersOfOverflownObjectToNewPage(page, overflownObject, document);
				Page overflowToPage = getPageOf(overflowToContainer);

				// the siblings to move have to be collected before moving the overflownObject
				// otherwise the overflown object will be moved and the original siblings will become unknown
				List<VisualObject> siblingsToMove = new ArrayList<>();
				VisualObject sibling = overflownObject.getNextSibling();
				while (sibling != null) {
					siblingsToMove.add(sibling);
					sibling = sibling.getNextSibling();
				}

				// allow the overflownObject to provide an addition overflow strategy, to modify itself (like splitting tables)
				overflownObject.overflowVerticalInto(overflowToContainer, overflownObjectMaxHeight);

				// move the overflowed object and its siblings to the overflow container
				for (VisualObject visualObject : siblingsToMove) {
					overflowToContainer.add(visualObject);
				}

				// once all overflow relevant content is moved to the new page, the new page can be layouted
				overflowToPage.doLayout();
			} else {
				throw new IllegalStateException("there has to be a page instance in the visual object hierarchy.");
			}
		} else {
			throw new IllegalStateException("there has to be a document instance in the root of the visual object hierarchy.");
		}
	}
	
	/**
	 * 
	 * @param container the current container that shall be copied to the overflowToContainer
	 * @param overflownObject the visual object that caused the overflow. it is used to stop the recursion
	 * @param overflowToContainer the container, into which the objects shall be copied
	 * @return the copied container in the overflowContainer
	 */
	private Container cloneParentContainersOfOverflownObjectToNewPage(Container container, VisualObject overflownObject, Container overflowToContainer) {
		Container ownerContainer = overflownObject.getOwnerContainer();
		// recursively iterate to the pageRoot
		if (container != ownerContainer) {
			overflowToContainer = cloneParentContainersOfOverflownObjectToNewPage(container, ownerContainer, overflowToContainer);
		}

		// then start the copy / move process
		OverflowPage overflowPage = null;
		Container clonedOwnerContainer;
		if (overflowToContainer.is(Document.class)) {
			Document doc = overflowToContainer.as(Document.class);
			overflowPage = doc.getItem(OverflowPage.class);
			if (overflowPage != null) {
				Page page = overflowPage.getContext().createPage(overflowToContainer);
				overflowToContainer.add(page);
				clonedOwnerContainer = page;
			} else {
				clonedOwnerContainer = ownerContainer.copyToWithoutChildren(overflowToContainer).as(Container.class);
				doc.add(clonedOwnerContainer);
			}
		} else {
			clonedOwnerContainer = ownerContainer.copyToWithoutChildren(overflowToContainer).as(Container.class);
			overflowToContainer.add(clonedOwnerContainer);
		}

		if (clonedOwnerContainer.is(Page.class)) {
			Page page = ownerContainer.as(Page.class);
			// if there was an overflowPage defined, copy its template to the new page. otherwise use the original page template
			if (overflowPage != null) {
				page = overflowPage;
			}
			// deal with the template images and texts
			// that means copying the template objects to the new page
			List<PageTemplate> templates = page.getItems(PageTemplate.class);
			if (templates != null) {
				for (PageTemplate pageTemplate : templates) {
					pageTemplate.copyTo(clonedOwnerContainer);
				}
			}
		}

		VisualObject sibling = ownerContainer.getNextSibling();
		while (sibling != null) {
			overflowToContainer.add(sibling);
			sibling = sibling.getNextSibling();
		}

		return clonedOwnerContainer;
	}
}
