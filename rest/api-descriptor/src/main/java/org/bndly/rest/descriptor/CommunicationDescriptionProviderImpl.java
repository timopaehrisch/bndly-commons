package org.bndly.rest.descriptor;

/*-
 * #%L
 * REST API Descriptor
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

import org.bndly.rest.descriptor.util.TypeUtil;
import org.bndly.rest.descriptor.model.TypeBinding;
import org.bndly.rest.descriptor.model.TypeMember;
import org.bndly.rest.descriptor.model.TypeDescriptor;
import org.bndly.rest.descriptor.model.LinkDescriptor;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.atomlink.api.AtomLinkInjector;
import org.bndly.rest.atomlink.api.AtomLinkInjectorListener;
import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.BeanID;
import org.bndly.rest.atomlink.api.annotation.Reference;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { CommunicationDescriptionProviderImpl.class, AtomLinkInjectorListener.class }, immediate = true)
public class CommunicationDescriptionProviderImpl implements AtomLinkInjectorListener {

	private static final Logger LOG = LoggerFactory.getLogger(CommunicationDescriptionProviderImpl.class);
	private final Map<Class<?>, RegisteredJAXBMessageClassProvider> registeredMessageClassProvidersByRegisteredTypes = new HashMap<>();
	private final Map<Class<?>, List<RegisteredAtomLinkDescription>> atomLinkDescriptionsByLinkedInClass = new HashMap<>();
	private final Map<Class<?>, TypeDescriptor> typeDescritors = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final List<RegisteredJAXBMessageClassProvider> messageClassProviders = new ArrayList<>();

	private String xsdString;

	@Override
	public void addedAtomLink(AtomLinkDescription atomLinkDescription, AtomLinkInjector atomLinkInjector) {
		lock.writeLock().lock();
		try {
			List<RegisteredAtomLinkDescription> listOfRegisteredDescriptions = atomLinkDescriptionsByLinkedInClass.get(atomLinkDescription.getLinkedInClass());
			if (listOfRegisteredDescriptions == null) {
				listOfRegisteredDescriptions = new ArrayList<>();
				atomLinkDescriptionsByLinkedInClass.put(atomLinkDescription.getLinkedInClass(), listOfRegisteredDescriptions);
			} else {
				// test is we don't have to do a thing
				for (RegisteredAtomLinkDescription registeredAtomLinkDescription : listOfRegisteredDescriptions) {
					if (registeredAtomLinkDescription.getAtomLinkDescription() == atomLinkDescription) {
						return;
					}
				}
			}
			RegisteredAtomLinkDescription registeredAtomLinkDescription = new RegisteredAtomLinkDescription(atomLinkDescription);
			listOfRegisteredDescriptions.add(registeredAtomLinkDescription);
			TypeDescriptor td = typeDescritors.get(atomLinkDescription.getLinkedInClass());
			if (td != null) {
				createLinkDescriptorInTypeDescriptor(td, registeredAtomLinkDescription);
				registeredAtomLinkDescription.setDeferred(false);
			} else {
				LOG.debug("deferring link descriptor creation of atom link description, because the target type descriptor is not available yet");
				registeredAtomLinkDescription.setDeferred(true);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removedAtomLink(AtomLinkDescription atomLinkDescription, AtomLinkInjector atomLinkInjector) {
		lock.writeLock().lock();
		try {
			List<RegisteredAtomLinkDescription> listOfRegisteredDescriptions = atomLinkDescriptionsByLinkedInClass.get(atomLinkDescription.getLinkedInClass());
			if (listOfRegisteredDescriptions != null) {
				Iterator<RegisteredAtomLinkDescription> iterator = listOfRegisteredDescriptions.iterator();
				while (iterator.hasNext()) {
					RegisteredAtomLinkDescription next = iterator.next();
					if (next.getAtomLinkDescription() == atomLinkDescription) {
						iterator.remove();
					}
				}
				if (listOfRegisteredDescriptions.isEmpty()) {
					atomLinkDescriptionsByLinkedInClass.remove(atomLinkDescription.getLinkedInClass());
				}
			}
			TypeDescriptor td = typeDescritors.get(atomLinkDescription.getLinkedInClass());
			if (td != null) {
				Iterator<LinkDescriptor> iterator = td.getLinks().iterator();
				while (iterator.hasNext()) {
					LinkDescriptor linkDescriptor = iterator.next();
					if (linkDescriptor.getRel().equals(atomLinkDescription.getRel())) {
						iterator.remove();
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	@org.osgi.service.component.annotations.Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = JAXBMessageClassProvider.class,
			bind = "addJAXBMessageClassProvider",
			unbind = "removeJAXBMessageClassProvider"
	)
	public void addJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		if (messageClassProvider != null) {
			lock.writeLock().lock();
			try {
				Iterator<RegisteredJAXBMessageClassProvider> iterator = messageClassProviders.iterator();
				while (iterator.hasNext()) {
					RegisteredJAXBMessageClassProvider next = iterator.next();
					if (next.getMessageClassProvider() == messageClassProvider) {
						// skip
						return;
					}
				}
				// if we did not find a previous registered jaxbmessageclassprovider
				RegisteredJAXBMessageClassProvider r = new RegisteredJAXBMessageClassProvider(messageClassProvider);
				messageClassProviders.add(r);
				assertTypeDescriptorExistForCommunicationBeanTypesOfProvider(r);
				updateConsumesAndReturnsOnLinkDescriptors();
				generateXSD();
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		if (messageClassProvider != null) {
			lock.writeLock().lock();
			try {
				Iterator<RegisteredJAXBMessageClassProvider> iterator = messageClassProviders.iterator();
				while (iterator.hasNext()) {
					RegisteredJAXBMessageClassProvider next = iterator.next();
					if (next.getMessageClassProvider() == messageClassProvider) {
						iterator.remove();
						dropTypeDescriptorsOfMessageClassProvider(next);
						updateConsumesAndReturnsOnLinkDescriptors();
						generateXSD();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	private class RegisteredAtomLinkDescription {
		private final AtomLinkDescription atomLinkDescription;
		private boolean deferred;
		private LinkDescriptor link;

		public RegisteredAtomLinkDescription(AtomLinkDescription atomLinkDescription) {
			if (atomLinkDescription == null) {
				throw new IllegalArgumentException("messageClassProvider is not allowed to be null");
			}
			this.atomLinkDescription = atomLinkDescription;
		}

		public AtomLinkDescription getAtomLinkDescription() {
			return atomLinkDescription;
		}

		public boolean isDeferred() {
			return deferred;
		}

		public void setDeferred(boolean deferred) {
			this.deferred = deferred;
		}

		public void setLinkDescriptor(LinkDescriptor link) {
			this.link = link;
		}

		public LinkDescriptor getLinkDescriptor() {
			return link;
		}
		
	}

	private class RegisteredJAXBMessageClassProvider {

		private final JAXBMessageClassProvider messageClassProvider;
		private final List<TypeDescriptor> typeDescriptors = new ArrayList<>();

		public RegisteredJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
			if (messageClassProvider == null) {
				throw new IllegalArgumentException("messageClassProvider is not allowed to be null");
			}
			this.messageClassProvider = messageClassProvider;
		}

		public JAXBMessageClassProvider getMessageClassProvider() {
			return messageClassProvider;
		}

		public List<TypeDescriptor> getTypeDescriptors() {
			return typeDescriptors;
		}

	}

	@Activate
	public void activate() {
		LOG.info("activating communication description provider");
		LOG.info("activated communication description provider");
	}

	@Deactivate
	public void deactivate() {
		LOG.info("deactivating communication description provider");
		lock.writeLock().lock();
		try {
			for (RegisteredJAXBMessageClassProvider registeredJAXBMessageClassProvider : messageClassProviders) {
				List<TypeDescriptor> tds = registeredJAXBMessageClassProvider.getTypeDescriptors();
				if (tds != null) {
					for (TypeDescriptor typeDescriptor : tds) {
						typeDescriptor.getLinks().clear();
					}
				}
			}
			messageClassProviders.clear();
			registeredMessageClassProvidersByRegisteredTypes.clear();
			atomLinkDescriptionsByLinkedInClass.clear();
		} finally {
			lock.writeLock().unlock();
		}
		LOG.info("deactivated communication description provider");
	}

	private void dropTypeDescriptorsOfMessageClassProvider(RegisteredJAXBMessageClassProvider typeDescriptorOwner) {
		List<TypeDescriptor> typeDescriptors = typeDescriptorOwner.getTypeDescriptors();
		if (typeDescriptors != null && !typeDescriptors.isEmpty()) {
			final Iterator<TypeDescriptor> iterator = typeDescriptors.iterator();
			while (iterator.hasNext()) {
				TypeDescriptor typeDescriptor = iterator.next();
				iterator.remove();
				typeDescritors.remove(typeDescriptor.getJavaType());
				registeredMessageClassProvidersByRegisteredTypes.remove(typeDescriptor.getJavaType());
				// defer the atom link descriptions
				List<RegisteredAtomLinkDescription> registeredAtomLinkDescriptions = atomLinkDescriptionsByLinkedInClass.get(typeDescriptor.getJavaType());
				if (registeredAtomLinkDescriptions != null) {
					for (RegisteredAtomLinkDescription registeredAtomLinkDescription : registeredAtomLinkDescriptions) {
						registeredAtomLinkDescription.setDeferred(true);
					}
				}
				// remove type descriptor as the parent from its children
				List<TypeDescriptor> subs = typeDescriptor.getSubs();
				if (subs != null && !subs.isEmpty()) {
					final Iterator<TypeDescriptor> iter = subs.iterator();
					while (iter.hasNext()) {
						TypeDescriptor sub = iter.next();
						sub.setParent(null);
					}
				}
				// remove type descriptor as a child from its parent
				TypeDescriptor parent = typeDescriptor.getParent();
				if (parent != null) {
					List<TypeDescriptor> parentSubs = parent.getSubs();
					if (parentSubs != null) {
						Iterator<TypeDescriptor> parentSubIterator = parentSubs.iterator();
						while (parentSubIterator.hasNext()) {
							if (parentSubIterator.next() == typeDescriptor) {
								parentSubIterator.remove();
							}
						}
					}
				}
			}
		}
	}

	private void assertTypeDescriptorExistForCommunicationBeanTypesOfProvider(RegisteredJAXBMessageClassProvider messageClassProvider) {
		// analyzes links between the different java types
		for (Class<?> communicationBeanType : messageClassProvider.getMessageClassProvider().getJAXBMessageClasses()) {
			TypeDescriptor created = assertTypeDescriptorExistsForJavaCommunicationBeanType(communicationBeanType, messageClassProvider);
			// then iterate all other type descriptors, that are missing their parents
			for (Map.Entry<Class<?>, TypeDescriptor> entry : typeDescritors.entrySet()) {
				Class<?> sc = entry.getKey().getSuperclass();
				if (communicationBeanType.equals(sc)) {
					TypeDescriptor td = entry.getValue();
					td.setParent(created);
					created.getSubs().add(td);
				}
			}
		}
	}

	public Map<Class<?>, TypeDescriptor> getTypeDescriptors() {
		return typeDescritors;
	}

	private TypeDescriptor assertTypeDescriptorExistsForJavaCommunicationBeanType(Class<?> communicationBeanType, RegisteredJAXBMessageClassProvider messageClassProvider) {
		TypeDescriptor td;
		lock.readLock().lock();
		try {
			td = typeDescritors.get(communicationBeanType);
			if (td != null) {
				return td;
			}
		} finally {
			lock.readLock().unlock();
		}
		// we did not find it yet, so we have to create the descriptor
		lock.writeLock().lock();
		try {
			// try to get it again, because another thread might already have created it before we obtained the write lock
			td = typeDescritors.get(communicationBeanType);
			if (td != null) {
				return td;
			}
			td = new TypeDescriptor(communicationBeanType);
			td.setName(communicationBeanType.getSimpleName());
			td.setSubs(new ArrayList<TypeDescriptor>());
			td.setMembers(new ArrayList<TypeMember>());
			td.setLinks(new ArrayList<LinkDescriptor>());
			typeDescritors.put(communicationBeanType, td);
			registeredMessageClassProvidersByRegisteredTypes.put(communicationBeanType, messageClassProvider);
			if (messageClassProvider.getMessageClassProvider().getJAXBMessageClasses().contains(communicationBeanType)) {
				messageClassProvider.getTypeDescriptors().add(td);
			}

			boolean isReferenceType = communicationBeanType.isAnnotationPresent(Reference.class);
			td.setReferenceType(isReferenceType);

			XmlRootElement rootElementAnnotation = communicationBeanType.getAnnotation(XmlRootElement.class);
			String rootElement = rootElementAnnotation != null ? rootElementAnnotation.name() : null;
			td.setRootElement(rootElement);

			XmlAccessorType accessorTypeAnnotation = communicationBeanType.getAnnotation(XmlAccessorType.class);
			if (accessorTypeAnnotation != null) {
				XmlAccessType accessType = accessorTypeAnnotation.value();
				if (!XmlAccessType.NONE.equals(accessType)) {
					throw new IllegalStateException("unsupported XmlAccessType: " + accessType);
				}

				Class<?> superType = communicationBeanType.getSuperclass();
				if (superType != null && !superType.equals(Object.class)) {
					if (messageClassProvider.getMessageClassProvider().getJAXBMessageClasses().contains(superType)) {
						TypeDescriptor parent = assertTypeDescriptorExistsForJavaCommunicationBeanType(superType, messageClassProvider);
						if (parent != null) {
							parent.getSubs().add(td);
						}
						td.setParent(parent);
					} else {
						TypeDescriptor parent = typeDescritors.get(superType);
						if (parent != null) {
							parent.getSubs().add(td);
							td.setParent(parent);
						} else {
							LOG.info("type descriptor communication bean type parent {} does not exist yet", superType.getName());
						}
					}
				}

				Field[] declaredFields = communicationBeanType.getDeclaredFields();
				for (Field field : declaredFields) {
					addTypeMemberFromJavaFieldDeclaration(field, td, messageClassProvider);
				}
			}
			
			// let deffered atom link descriptions be created
			List<RegisteredAtomLinkDescription> registeredAtomLinkDescriptions = atomLinkDescriptionsByLinkedInClass.get(communicationBeanType);
			if (registeredAtomLinkDescriptions != null) {
				for (RegisteredAtomLinkDescription registeredAtomLinkDescription : registeredAtomLinkDescriptions) {
					if (registeredAtomLinkDescription.isDeferred()) {
						createLinkDescriptorInTypeDescriptor(td, registeredAtomLinkDescription);
						registeredAtomLinkDescription.setDeferred(false);
					}
				}
			}
			
			return td;
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void createLinkDescriptorInTypeDescriptor(TypeDescriptor td, RegisteredAtomLinkDescription registeredAtomLinkDescription) {
		AtomLinkDescription binding = registeredAtomLinkDescription.getAtomLinkDescription();
		for (LinkDescriptor linkDescriptor : td.getLinks()) {
			if (linkDescriptor.getRel().equals(binding.getRel())) {
				return;
			}
		}
		LinkDescriptor link = new LinkDescriptor();
		link.setAuthors(binding.getAuthors());
		link.setDescription(binding.getHumanReadableDescription());
		link.setRel(binding.getRel());
		setConsumesTypeDescriptor(link, binding);
		setReturnsTypeDescriptor(link, binding);
		td.getLinks().add(link);
		registeredAtomLinkDescription.setLinkDescriptor(link);
	}
	
	private void updateConsumesAndReturnsOnLinkDescriptors() {
		for (List<RegisteredAtomLinkDescription> registeredAtomLinkDescriptions : atomLinkDescriptionsByLinkedInClass.values()) {
			for (RegisteredAtomLinkDescription registeredAtomLinkDescription : registeredAtomLinkDescriptions) {
				LinkDescriptor link = registeredAtomLinkDescription.getLinkDescriptor();
				if (link != null) {
					AtomLinkDescription binding = registeredAtomLinkDescription.getAtomLinkDescription();
					setConsumesTypeDescriptor(link, binding);
					setReturnsTypeDescriptor(link, binding);
				}
			}
		}
	}
	
	private void setConsumesTypeDescriptor(LinkDescriptor link, AtomLinkDescription binding) {
		Class<?> consumedType = binding.getConsumesType();
		if (consumedType != null) {
			TypeDescriptor consumes = typeDescritors.get(consumedType);
			link.setConsumes(consumes);
		}
	}
	private void setReturnsTypeDescriptor(LinkDescriptor link, AtomLinkDescription binding) {
		Class<?> implicitReturnType = binding.getReturnType();
		if (implicitReturnType != null) {
			if (!TypeUtil.isSimpleType(implicitReturnType) && implicitReturnType.isAnnotationPresent(XmlRootElement.class)) {
				TypeDescriptor returns = typeDescritors.get(implicitReturnType);
				link.setReturns(returns);
			}
		}
	}

	private void addTypeMemberFromJavaFieldDeclaration(Field field, TypeDescriptor typeDescriptor, RegisteredJAXBMessageClassProvider messageClassProvider) {
		List<XmlElement> elements = new ArrayList<>();

		XmlElements xmlElements = field.getAnnotation(XmlElements.class);
		XmlElement xmlElement = null;
		if (xmlElements == null) {
			xmlElement = field.getAnnotation(XmlElement.class);
			if (xmlElement != null) {
				elements.add(xmlElement);
			}
		} else {
			elements.addAll(Arrays.asList(xmlElements.value()));
		}

		if (!elements.isEmpty()) {
			String memberName = field.getName();
			Class<?> memberType = field.getType();
			boolean isBeanId = field.isAnnotationPresent(BeanID.class);
			boolean isCollection = TypeUtil.isCollection(memberType);
			if (isCollection) {
				memberType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
			}
			if (!TypeUtil.isSimpleType(memberType)) {
				if (messageClassProvider.getMessageClassProvider().getJAXBMessageClasses().contains(memberType)) {
					assertTypeDescriptorExistsForJavaCommunicationBeanType(memberType, messageClassProvider);
				}
			}
			List<TypeBinding> bindings = new ArrayList<>();
			for (XmlElement el : elements) {
				TypeBinding binding = new TypeBinding();
				String name = el.name();
				if ("##default".equals(name)) {
					name = field.getName();
				}
				binding.setName(name);
				Class<?> type = el.type();
				if (type.equals(XmlElement.DEFAULT.class)) {
					type = memberType;
				}
				binding.setType(type);
				bindings.add(binding);
			}
			TypeMember tm = new TypeMember();
			tm.setName(memberName);
			tm.setJavaType(memberType);
			tm.setBindings(bindings);
			tm.setBeanId(isBeanId);
			tm.setCollection(isCollection);
			typeDescriptor.getMembers().add(tm);
		}
	}

	public String getXsdString() {
		return xsdString;
	}

	public void generateXSD() {
		Set<Class> knownTypes = new HashSet<>();
		Iterator<RegisteredJAXBMessageClassProvider> iter = registeredMessageClassProvidersByRegisteredTypes.values().iterator();
		while (iter.hasNext()) {
			RegisteredJAXBMessageClassProvider next = iter.next();
			JAXBMessageClassProvider prov = next.messageClassProvider;
			Collection<Class<?>> classes = prov.getJAXBMessageClasses();
			for (Class<?> clazz : classes) {
				ClassLoader cl = clazz.getClassLoader();
				if (BundleReference.class.isInstance(cl)) {
					BundleReference bundleReference = (BundleReference) cl;
					Bundle bundle = bundleReference.getBundle();
					if (Bundle.UNINSTALLED == bundle.getState()) {
						continue;
					}
				}
				knownTypes.add(clazz);
			}
		}
		Class[] classes = knownTypes.toArray(new Class[knownTypes.size()]);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(classes);
			LOG.trace("Successfully built JAXBContext with provided classes");
			final StringWriter sw = new StringWriter();
			SchemaOutputResolver or = new SchemaOutputResolver() {

				@Override
				public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
					StreamResult res = new StreamResult(sw);
					res.setSystemId("no-id");
					return res;
				}
			};
			jaxbContext.generateSchema(or);
			sw.flush();
			xsdString = sw.toString();
			LOG.trace("Successfully generated schema");
		} catch (Exception ex) {
			LOG.error("failed to generate XSD: " + ex.getMessage(), ex);
		}
	}

}
