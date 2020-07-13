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

import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.descriptor.util.JAXRSUtil;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescriptor;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.atomlink.api.annotation.PathParameter;
import org.bndly.rest.atomlink.api.Fragment;
import org.bndly.rest.atomlink.api.VariableFragment;
import org.bndly.rest.atomlink.api.annotation.CompiledAtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.QueryParameter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PathParameterExtractorImpl.class)
public class PathParameterExtractorImpl {

	private static final Logger LOG = LoggerFactory.getLogger(PathParameterExtractorImpl.class);
	public static final String NAME = "pathParameterExtractor";
	
	@Reference
	private DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;

	public ParameterValue[] extractPathParameters(CompiledAtomLinkDescription binding, String url) {
		List<PathParameter> pathParameters = binding.getPathParams();
		if (pathParameters == null || pathParameters.isEmpty()) {
			return null;
		}
		List<Fragment> uriTemplateFragments = binding.getUriFragments();
		List<ParameterValue> fragmentValues = new ArrayList<>();
		int j = 0;
		ResourceURI uri = new ResourceURIParser(defaultCharacterEncodingProvider.createPathCoder(), url).parse().getResourceURI();
		if (uri.hasSchemeHost()) {
			// find the beginning of the uriTemplate in the provided url. 
			// the assumption is, that the first fragment is always static and 
			// does not appear before the actual relative link start
			Fragment firstFragment = uriTemplateFragments.get(0);
			int fragmentStart = url.indexOf(firstFragment.getValue());
			url = url.substring(fragmentStart);
		} else {
			// we assume, that the url has been preprocessed already
		}
		if (url.startsWith("/")) {
			url = url.substring(1);
		}
		
		for (Fragment fragment : uriTemplateFragments) {
			if (VariableFragment.class.isInstance(fragment)) {
                // since we dont know how long a concrete value for a variable is, 
				// the end of a variable is determined by the following static fragment.
				Fragment nextFragment = null;
				if (j + 1 < uriTemplateFragments.size()) {
					nextFragment = uriTemplateFragments.get(j + 1);
				}

				String varName = ((VariableFragment) fragment).getVariableName();
				if (nextFragment == null) {
					// the variable reaches the end of the URL
					String variableValue = url;
					fragmentValues.add(new ParameterValue(varName, variableValue));

				} else {
					// the variable ends at the start of nextFragment
					int nextFragmentStart = url.indexOf(nextFragment.getValue());
					if (nextFragmentStart > -1) {
						String variableValue = url.substring(0, nextFragmentStart);
						fragmentValues.add(new ParameterValue(varName, variableValue));
						url = url.substring(variableValue.length());
					} else {
						break;
					}
				}

			} else {
				if (url.startsWith(fragment.getValue())) {
					url = url.substring(fragment.getValue().length());
				} else {
					break;
				}
			}
			j++;
		}
		return fragmentValues.toArray(new ParameterValue[fragmentValues.size()]);
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////
	// stuff used to analyze the resourceControllers and their relations to the restBeans
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void processMethods(Method[] methods, List<CompiledAtomLinkDescription> r, Object resourceInstance, String segment) {
		if (methods != null && methods.length > 0) {
			processMethods(Arrays.asList(methods), r, resourceInstance, segment);
		}
	}

	public void processMethods(Collection<Method> methods, List<CompiledAtomLinkDescription> r, Object resourceInstance, String segment) {
		if (methods != null) {
			for (Method method : methods) {
				processMethod(method, r, resourceInstance, segment);
			}
		}
	}

	public void processMethod(Method method, List<CompiledAtomLinkDescription> r, Class resourceClass, String segment) {
		processMethod(method, r, null, resourceClass, segment);
	}

	public void processMethod(Method method, List<CompiledAtomLinkDescription> r, Object resourceInstance, String segment) {
		processMethod(method, r, resourceInstance, null, segment);
	}

	public void processMethod(Method method, List<CompiledAtomLinkDescription> r, Object resourceInstance, Class resourceClass, String segment) {
		if (method != null) {
			AtomLink atomLinkAnnotation = method.getAnnotation(AtomLink.class);
			if (atomLinkAnnotation == null) {
				AtomLinks linkResourcesAnnotation = method.getAnnotation(AtomLinks.class);
				if (linkResourcesAnnotation != null) {
					AtomLink[] linkAnnotations = linkResourcesAnnotation.value();
					if (linkAnnotations != null) {
						for (AtomLink link : linkAnnotations) {
							createAtomLinkBinding(link, method, r, resourceInstance, resourceClass, segment);
						}
					}
				}
			} else {
				createAtomLinkBinding(atomLinkAnnotation, method, r, resourceInstance, resourceClass, segment);
			}
		}
	}

	private void createAtomLinkBinding(AtomLink atomLinkAnnotation, Method method, List<CompiledAtomLinkDescription> r, Object resourceInstance, Class resourceClass, final String segment) {
		if (atomLinkAnnotation != null) {
			Class<?> descriptorClass = atomLinkAnnotation.descriptor();
			AtomLinkDescription desc;
			if (!descriptorClass.equals(Void.class) && AtomLinkDescriptor.class.isAssignableFrom(descriptorClass)) {
				AtomLinkDescriptor d;
				if (descriptorClass.isMemberClass()) {
					if (resourceInstance == null) {
						return;
					}
					try {
						d = (AtomLinkDescriptor) descriptorClass.getConstructor(resourceInstance.getClass()).newInstance(resourceInstance);
					} catch (
						InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex
					) {
						LOG.error("could not instantiate atom link descriptor class {}", descriptorClass.getName());
						return;
					}
				} else {
					try {
						Constructor<?> defaultConstructor = descriptorClass.getConstructor();
						try {
							d = (AtomLinkDescriptor) defaultConstructor.newInstance();
						} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
							return;
						}
					} catch (NoSuchMethodException | SecurityException ex) {
						if (resourceInstance != null) {
							try {
								Constructor<?> constructor = descriptorClass.getConstructor(resourceInstance.getClass());
								try {
									d = (AtomLinkDescriptor) constructor.newInstance(resourceInstance);
								} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex1) {
									return;
								}
							} catch (NoSuchMethodException | SecurityException ex1) {
								return;
							}
						} else {
							return;
						}
					}
				}
				desc = d.getAtomLinkDescription(resourceInstance, method, atomLinkAnnotation);
			} else {
				desc = new DefaultAtomLinkDescriptor().getAtomLinkDescription(resourceInstance, method, atomLinkAnnotation);
			}

			if (desc != null && desc.getLinkedInClass() != null) {
				String tmpSegment = desc.getSegment();
				if (tmpSegment == null && segment != null) {
					desc = new DelegatingAtomLinkDescription(desc) {

						@Override
						public String getSegment() {
							return segment;
						}

					};
				}
				r.add(new CompiledAtomLinkDescriptionImpl(desc));
			}
		}
	}

	private static class CompiledAtomLinkDescriptionImpl extends DelegatingAtomLinkDescription implements CompiledAtomLinkDescription {

		private final List<Fragment> fragments;
		private final String uriTemplate;
		private final Map<String, QueryParameter> queryParamsByName;
		private final List<PathParameter> pathParams;

		public CompiledAtomLinkDescriptionImpl(AtomLinkDescription atomLinkDescription) {
			super(atomLinkDescription);
			String uriTemplateForClass = JAXRSUtil.extractURITemplateForMethod(atomLinkDescription.getControllerMethod());
			String segment = atomLinkDescription.getSegment();
			if (segment != null && !segment.isEmpty()) {
				if (uriTemplateForClass.startsWith("/")) {
					if (segment.endsWith("/")) {
						uriTemplateForClass = segment.substring(0, segment.length() - 1) + uriTemplateForClass;
					} else {
						uriTemplateForClass = segment + uriTemplateForClass;
					}
				} else {
					if (segment.endsWith("/")) {
						uriTemplateForClass = segment + uriTemplateForClass;
					} else {
						uriTemplateForClass = segment + "/" + uriTemplateForClass;
					}
				}
			}
			this.uriTemplate = uriTemplateForClass;
			this.fragments = JAXRSUtil.splitIntoFragments(uriTemplateForClass);
			List<QueryParameter> params = atomLinkDescription.getQueryParams();
			if (params != null && !params.isEmpty()) {
				Map<String, QueryParameter> hashMap = new LinkedHashMap<>();
				for (QueryParameter param : params) {
					hashMap.put(param.getName(), param);
				}
				queryParamsByName = Collections.unmodifiableMap(hashMap);
			} else {
				queryParamsByName = Collections.EMPTY_MAP;
			}
			final Set<String> pathVariableNames = new HashSet<>();
			for (Fragment fragment : fragments) {
				if (VariableFragment.class.isInstance(fragment)) {
					String variableName = ((VariableFragment) fragment).getVariableName();
					pathVariableNames.add(variableName);
				}
			}
			for (PathParameter pathParameter : atomLinkDescription.getPathParams()) {
				pathVariableNames.remove(pathParameter.getName());
			}
			List<PathParameter> tmp = new ArrayList<>(atomLinkDescription.getPathParams());
			// for the remaining variables, create the path paramters here
			for (final String pathVariableName : pathVariableNames) {
				tmp.add(new PathParameter() {

					@Override
					public String getName() {
						return pathVariableName;
					}

					@Override
					public String getHumanReadableDescription() {
						return null;
					}
				});
			}
			this.pathParams = Collections.unmodifiableList(tmp);
		}

		@Override
		public List<Fragment> getUriFragments() {
			return fragments;
		}

		@Override
		public String getUriTemplate() {
			return uriTemplate;
		}

		@Override
		public List<PathParameter> getPathParams() {
			return pathParams;
		}

		@Override
		public Map<String, QueryParameter> getQueryParamsByName() {
			return queryParamsByName;
		}
		
	}
}
