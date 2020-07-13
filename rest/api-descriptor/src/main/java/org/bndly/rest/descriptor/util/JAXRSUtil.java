package org.bndly.rest.descriptor.util;

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

import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.atomlink.api.Fragment;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.QueryParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.atomlink.api.StaticFragment;
import org.bndly.rest.atomlink.api.VariableFragment;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class JAXRSUtil {

	private JAXRSUtil() {
	}
	
	public static interface JavaMethodParameterIteratorCallback {
		public static interface Context {
			<A extends Annotation> A getAnnotation(Class<A> annotationType);
		}
		void onParameter(Context context);
	}
	
	public static Class<?> extractImplicitReturnType(Method method) {
		Class<?> returnType = method.getReturnType();
		if (Response.class.isAssignableFrom(returnType)) {

			GET get = method.getAnnotation(GET.class);
			if (get != null) {
				// only reading access should have an implicit return type
				AtomLink linkResourceAnnotation = method.getAnnotation(AtomLink.class);
				if (linkResourceAnnotation == null) {
					AtomLinks linkResourcesAnnotation = method.getAnnotation(AtomLinks.class);
					if (linkResourcesAnnotation != null) {
						AtomLink[] linkAnnotations = linkResourcesAnnotation.value();
						if (linkAnnotations != null) {
							for (AtomLink linkResource : linkAnnotations) {
								String linkRel = linkResource.rel();
								if ("".equals(linkRel) || "self".equals(linkRel)) {
									linkResourceAnnotation = linkResource;
									break;
								}
							}
						}
					}
				}

				if (linkResourceAnnotation == null) {
					return returnType;
				}
				Class<?> linkInType = linkResourceAnnotation.target();
				if (!Void.class.equals(linkInType)) {
					return linkInType;
				}
			}
		}
		return returnType;
	}
	
	public static String extractURITemplateForMethod(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();

		StringBuffer sb = new StringBuffer();
		String classPath = "";
		Path pathAnnotationOnClass = declaringClass.getAnnotation(Path.class);
		if (pathAnnotationOnClass != null) {
			classPath = pathAnnotationOnClass.value();
			sb.append(classPath);
		}

		String methodPath = "";
		Path pathAnnotationOnMethod = method.getAnnotation(Path.class);
		if (pathAnnotationOnMethod != null) {
			if (!classPath.endsWith("/") && !classPath.equals("")) {
				sb.append('/');
			}
			methodPath = pathAnnotationOnMethod.value();
			sb.append(methodPath);
		}

		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		List<String> qps = new ArrayList<String>();
		for (Annotation[] annotations : parameterAnnotations) {
			for (Annotation annotation : annotations) {
				if (QueryParam.class.isAssignableFrom(annotation.getClass())) {
					QueryParam qp = QueryParam.class.cast(annotation);
					qps.add(qp.value());
				}
			}
		}

		if (qps.size() > 0) {
			StringBuffer qpSb = null;
			for (String qp : qps) {
				if (qpSb == null) {
					qpSb = new StringBuffer();
					qpSb.append('?');
				} else {
					qpSb.append('&');
				}
				qpSb.append(qp);
				qpSb.append("={");
				qpSb.append(qp);
				qpSb.append('}');
			}
			sb.append(qpSb);
		}

		String uriTemplateForClass = sb.toString();
		return uriTemplateForClass;
	}
	
	/**
	 * splits a uri template into fragments of static and variable parts. variable parts are marked with { and } symbols. example "{varname}"
	 * @param uriTemplate
	 * @return
	 */
	public static List<Fragment> splitIntoFragments(String uriTemplate) {
		List<Fragment> fragments = new ArrayList<>();
		
		int i = 0;
		int si = uriTemplate.indexOf('{');
		int ei = uriTemplate.indexOf('}');
		if (si > -1 && si < ei) {
			while (si > -1 && si < ei) {
				Fragment f = buildFragment(uriTemplate.substring(i, si));
				fragments.add(f);
				f = buildFragment(uriTemplate.substring(si, ei + 1));
				fragments.add(f);
				i = ei + 1;
				si = uriTemplate.indexOf('{', i);
				ei = uriTemplate.indexOf('}', i);
			}
		}

		if (i < uriTemplate.length()) {
			Fragment f = buildFragment(uriTemplate.substring(i, uriTemplate.length()));
			fragments.add(f);
		}

		return fragments;
	}

	private static Fragment buildFragment(String fragmentString) {
		String var = getVariableNameFromFragment(fragmentString);
		// if the string is the same instance, there is no variable.
		if (var == fragmentString) {
			return new StaticFragment(fragmentString);
		} else {
			return new VariableFragment(fragmentString, var);
		}
	}

	public static boolean isVariableFragment(String fragment) {
		return fragment.startsWith("{") && fragment.endsWith("}");
	}

	public static String getVariableNameFromFragment(String fragment) {
		if (isVariableFragment(fragment)) {
			return fragment.substring(1, fragment.length() - 1);
		}
		return fragment;
	}
	
	public static int countVariables(String uriTemplate) {
		return countVariables(splitIntoFragments(uriTemplate));
	}
	
	public static int countVariables(List<Fragment> uriTemplateFragments) {
		int i = 0;
		for (Fragment f : uriTemplateFragments) {
			if (VariableFragment.class.isInstance(f)) {
				i++;
			}
		}
		return i;
	}

	/**
	 * takes an uriTemplate like "/persons/{pid}/wishLists/{id}" and extracts "pid" and "id" as variable names
	 * @param uriTemplate
	 * @return
	 */
	public static List<String> extractURITemplateVariables(String uriTemplate) {
		List<String> variables = new ArrayList<String>();

		List<Fragment> fragments = splitIntoFragments(uriTemplate);
		for (Fragment fragment : fragments) {
			if (VariableFragment.class.isInstance(fragment)) {
				variables.add(VariableFragment.class.cast(fragment).getVariableName());
			}
		}
		return variables;
	}

	/**
	 * takes an uri template and replaces the first occurence of that variable in the uri template. the returned object is the uri template with the replaced variable.
	 * @param uriRaw the uri template (like <code>persons/{pid}/wishLists?start={start}&amp;size={size}</code>)
	 * @param variableName the variable that should be replaced with a value (like "pid" or "start")
	 * @param variableValue the value that should be inserted as a replacement. null-values will be transformed to an empty string
	 * @return
	 */
	public static String engraveURITemplateVariable(String uriRaw, String variableName, Object variableValue) {
		String newUri;
		String escapedVariable = "{" + variableName + "}";
		int si = uriRaw.indexOf(escapedVariable);
		int ei = si + escapedVariable.length();

		if (variableValue == null) {
			variableValue = "";
		}

		try {
			String encoding = "UTF-8";
			String stringVariableValue = URLEncoder.encode(variableValue.toString(), encoding);
			newUri = uriRaw.substring(0, si) + stringVariableValue + uriRaw.substring(ei);
			return newUri;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("could not build an URI because the used encoding for escaping URL parameters is not supported", e);
		}
	}

	public static List<PathParam> extractPathParametersForMethod(Method method) {
		List<PathParam> list = extractAnnotationsFromMethod(method, PathParam.class);
		return list;
	}

	public static List<QueryParam> extractQueryParametersForMethod(Method method) {
		List<QueryParam> list = extractAnnotationsFromMethod(method, QueryParam.class);
		return list;
	}

	public static <T extends Annotation> List<T> extractAnnotationsFromMethod(Method method, final Class<T> annotationType) {
		final List<T> list = new ArrayList<>();
		iterateAnnotationsFromMethod(method, new JavaMethodParameterIteratorCallback() {

			@Override
			public void onParameter(JavaMethodParameterIteratorCallback.Context context) {
				T annotation = context.getAnnotation(annotationType);
				if (annotation != null) {
					list.add(annotation);
				}
			}
		});
		return list;
	}
	
	public static void iterateAnnotationsFromMethod(Method method, JavaMethodParameterIteratorCallback cb) {
		final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		final int[] pos = new int[1];
		JavaMethodParameterIteratorCallback.Context ctx = new JavaMethodParameterIteratorCallback.Context() {

			@Override
			public <A extends Annotation> A getAnnotation(Class<A> sibblingAnnotation) {
				Annotation[] annotations = parameterAnnotations[pos[0]];
				for (Annotation annotation : annotations) {
					if (sibblingAnnotation.isInstance(annotation)) {
						return (A) annotation;
					}
				}
				return null;
			}
		};
		pos[0] = 0;
		for (Annotation[] annotations : parameterAnnotations) {
			cb.onParameter(ctx);
			pos[0] = pos[0] + 1;
		}
	}

}
