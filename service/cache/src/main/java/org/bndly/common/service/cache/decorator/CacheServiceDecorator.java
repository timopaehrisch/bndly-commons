package org.bndly.common.service.cache.decorator;

/*-
 * #%L
 * Service Cache
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

import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.common.service.cache.ChainedCache;
import org.bndly.common.service.cache.EntityIdentityKeyParameter;
import org.bndly.common.service.cache.SimpleKeyParameter;
import org.bndly.common.service.cache.api.ApplicationCache;
import org.bndly.common.service.cache.api.Cache;
import org.bndly.common.service.cache.api.CacheKey;
import org.bndly.common.service.cache.api.CacheKeyParameter;
import org.bndly.common.service.cache.api.CacheKeyProvider;
import org.bndly.common.service.cache.api.CacheLevel;
import org.bndly.common.service.cache.api.CacheLocaleProvider;
import org.bndly.common.service.cache.api.Cached;
import org.bndly.common.service.cache.api.EntityCache;
import org.bndly.common.service.cache.api.EntityCacheKey;
import org.bndly.common.service.cache.api.KeyParameter;
import org.bndly.common.service.cache.api.MethodInvocationCacheKey;
import org.bndly.common.service.cache.api.RequestCache;
import org.bndly.common.service.cache.keys.EntityCacheKeyImpl;
import org.bndly.common.service.cache.keys.LocalizedEntityCacheKeyImpl;
import org.bndly.common.service.cache.keys.LocalizedMethodInvocationCacheKeyImpl;
import org.bndly.common.service.cache.keys.MethodInvocationCacheKeyImpl;
import org.bndly.common.service.model.api.Identity;
import org.bndly.common.service.model.api.IdentityBuilder;
import org.bndly.common.service.model.api.ReferableResource;
import org.bndly.common.service.decorator.api.ServiceDecorator;
import org.bndly.common.service.decorator.api.ServiceDecoratorChain;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CacheServiceDecorator implements ServiceDecorator {

	private RequestCache requestCache;
	private ApplicationCache applicationCache;
	private EntityCache entityCache;
	private CacheLocaleProvider cacheLocaleProvider;

	private Method getReallyInvokedMethod(Method method, Object wrappedInstance) {
		Method invoked = method;
		Cached cachedAnnotation = method.getAnnotation(Cached.class);
		if (cachedAnnotation == null) {
			Class<? extends Object> instanceType = wrappedInstance.getClass();
			List<Method> methods = ReflectionUtil.listAnnotatedMethodsImplementedBy(instanceType, Cached.class);
			if (methods != null) {
				for (Method m : methods) {
					if (m.getName().equals(method.getName()) && m.getParameterTypes().length == method.getParameterTypes().length) {
						invoked = m;
						break;
					}
				}
			}
		}
		return invoked;
	}

	private Cached getCachedAnnotationForMethod(Method method, Object wrappedInstance) {
		return getReallyInvokedMethod(method, wrappedInstance).getAnnotation(Cached.class);
	}

	@Override
	public boolean appliesTo(Method method, Object wrappedInstance) {
		Cached cachedAnnotation = getCachedAnnotationForMethod(method, wrappedInstance);
		if (cachedAnnotation == null) {
			return false;
		}

		boolean returnsSomething = !method.getReturnType().equals(Void.class);
		if (!returnsSomething) {
			return false;
		}

		boolean requestCachingSupported = false;
		boolean applicationCachingSupported = false;
		boolean entityCachingSupported = false;
		CacheLevel[] levels = cachedAnnotation.levels();
		for (CacheLevel cacheLevel : levels) {
			if (CacheLevel.REQUEST.equals(cacheLevel) && requestCache != null) {
				requestCachingSupported = true;
			}
			if (CacheLevel.APPLICATION.equals(cacheLevel) && applicationCache != null) {
				applicationCachingSupported = true;
			}
			if (CacheLevel.ENTITY.equals(cacheLevel) && entityCache != null) {
				applicationCachingSupported = true;
			}
		}

		return (requestCachingSupported || applicationCachingSupported || entityCachingSupported);
	}

	@Override
	public boolean precedes(ServiceDecorator decorator) {
		// we want the cache to be placed in front of the actual invocation decorator
		return decorator.getClass().getSimpleName().equals("InvocationServiceDecorator");
	}

	@Override
	public Object execute(ServiceDecoratorChain decoratorChain, Object invocationTarget, Method interfaceMethod, final Object... args) throws Throwable {
		final Method invokedMethod = getReallyInvokedMethod(interfaceMethod, invocationTarget);
		final String methodName = invocationTarget.getClass().getSimpleName() + "." + invokedMethod.getName();
		final List<KeyParameter> parameters = new ArrayList<>();
		Class<?> returnedEntityType = invokedMethod.getReturnType();
		if (Collection.class.isAssignableFrom(returnedEntityType)) {
			returnedEntityType = ReflectionUtil.getCollectionParameterType(invokedMethod.getGenericReturnType());
		}
		final String returnedEntityTypeName = returnedEntityType.getName();

		Annotation[][] allParameterAnnotations = invokedMethod.getParameterAnnotations();
		Class<?>[] allParameterTypes = invokedMethod.getParameterTypes();
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object parameterValue = args[i];
				Annotation[] parameterAnnotations = allParameterAnnotations[i];
				CacheKeyParameter cacheKeyParameterAnnotation = null;
				for (Annotation annotation : parameterAnnotations) {
					if (CacheKeyParameter.class.isInstance(annotation)) {
						cacheKeyParameterAnnotation = (CacheKeyParameter) annotation;
						break;
					}
				}
				if (cacheKeyParameterAnnotation != null) {
					String parameterName = cacheKeyParameterAnnotation.value();
					KeyParameter param;
					if (ReferableResource.class.isInstance(parameterValue)) {
						// entity
						Identity identity = IdentityBuilder.buildOrIdentity(parameterValue);
						if (identity == null) {
							// if no identity can be built, skip to the next parameter
							continue;
						}
						param = new EntityIdentityKeyParameter(parameterName, identity);
					} else {
						// simple type
						param = new SimpleKeyParameter(parameterName, parameterValue);
					}
					parameters.add(param);
				}

			}
		}
		Identity entityIdentityCandidate = null;
		for (KeyParameter keyParameter : parameters) {
			if (EntityIdentityKeyParameter.class.isInstance(keyParameter)) {
				entityIdentityCandidate = EntityIdentityKeyParameter.class.cast(keyParameter).getValue();
			}
		}
		final Identity passedInEntityIdentity = entityIdentityCandidate;

		final boolean localized = isLocalizedMethod(invokedMethod);
		// create a cacheKeyBuilder for this invocation
		CacheKeyProvider cacheKeyProvider = new CacheKeyProvider() {
			@Override
			public <T extends CacheKey> T getKey(Class<T> cacheKeyType) {
				String locale = null;
				if (localized) {
					locale = assertCurrentLocaleIsDefined();
				}
				if (MethodInvocationCacheKey.class.isAssignableFrom(cacheKeyType)) {
					if (localized) {
						return (T) new LocalizedMethodInvocationCacheKeyImpl(methodName, parameters, locale);
					} else {
						return (T) new MethodInvocationCacheKeyImpl(methodName, parameters);
					}
				} else if (EntityCacheKey.class.isAssignableFrom(cacheKeyType)) {
					if (passedInEntityIdentity == null) {
						throw new IllegalStateException(
							"trying to build a " + EntityCacheKey.class.getSimpleName() 
							+ " but there where no " + CacheKeyParameter.class.getSimpleName() 
							+ " annotations found or the annotated parameter was null or the annotated parameter could not return any identity."
						);
					}
					if (localized) {
						return (T) new LocalizedEntityCacheKeyImpl(returnedEntityTypeName, passedInEntityIdentity, locale);
					} else {
						return (T) new EntityCacheKeyImpl(returnedEntityTypeName, passedInEntityIdentity);
					}
				} else {
					throw new IllegalArgumentException("unsupported cache key type: " + cacheKeyType.getName());
				}
			}
		};

		// look up the requested method result in the cache
		Cached cachedAnnotation = getCachedAnnotationForMethod(invokedMethod, invocationTarget);
		Cache cacheChain = getCacheForMethod(cachedAnnotation);
		if (cacheChain.existsInCache(cacheKeyProvider)) {
			return cacheChain.getCachedValue(cacheKeyProvider);
		} else {
			// if there is no cache hit, call doContinue
			Object result = decoratorChain.doContinue();
			// store the result in the cache
			cacheChain.storeValue(cacheKeyProvider, result);
			// if the result is a referableResource or a collection of referableResources,
			// then add the referableResources to the entity cache
			insertResultToEntityCache(result, localized);
			return result;
		}
	}

	private String assertCurrentLocaleIsDefined() throws IllegalStateException {
		String locale;
		if (cacheLocaleProvider == null) {
			throw new IllegalStateException("can't use a localized cache, when there is no cacheLocaleProvider in the application context.");
		}
		locale = cacheLocaleProvider.getCacheLocale();
		if (locale == null) {
			throw new IllegalStateException("cacheLocaleProvider did return null as a locale.");
		}
		return locale;
	}

	private boolean isLocalizedMethod(Method invokedMethod) {
		Cached cachedAnnotation = invokedMethod.getAnnotation(Cached.class);
		if (cachedAnnotation != null) {
			return cachedAnnotation.localized();
		}
		return false;
	}

	private Cache getCacheForMethod(Cached cachedAnnotation) {
		CacheLevel[] levels = cachedAnnotation.levels();
		List<Cache> caches = new ArrayList<>();
		for (CacheLevel cacheLevel : levels) {
			if (CacheLevel.REQUEST.equals(cacheLevel)) {
				caches.add(requestCache);
			} else if (CacheLevel.APPLICATION.equals(cacheLevel)) {
				caches.add(applicationCache);
			} else if (CacheLevel.ENTITY.equals(cacheLevel)) {
				caches.add(entityCache);
			}
		}
		return new ChainedCache(caches);
	}

	private void insertResultToEntityCache(Object result, boolean localized) {
		if (entityCache != null && result != null) {
			if (ReferableResource.class.isInstance(result)) {
				addReferableResourceToEntityCache((ReferableResource) result, localized);
			} else if (Collection.class.isInstance(result)) {
				Collection c = (Collection) result;
				for (Object object : c) {
					if (ReferableResource.class.isInstance(object)) {
						addReferableResourceToEntityCache((ReferableResource) object, localized);
					}
				}
			}
		}
	}

	private void addReferableResourceToEntityCache(ReferableResource rr, boolean localized) {
		String locale = null;
		if (localized) {
			locale = assertCurrentLocaleIsDefined();
		}
		if (entityCache != null && rr != null) {
			String returnedEntityTypeName = rr.getClass().getName();
			List<Identity> identities = IdentityBuilder.buildAllPossible(rr);
			if (identities != null && !identities.isEmpty()) {
				for (Identity identity : identities) {
					EntityCacheKeyImpl k;
					if (localized) {
						k = new LocalizedEntityCacheKeyImpl(returnedEntityTypeName, identity, locale);
					} else {
						k = new EntityCacheKeyImpl(returnedEntityTypeName, identity);
					}
					final EntityCacheKeyImpl key = k;
					entityCache.storeValue(new CacheKeyProvider() {
						@Override
						public <T extends CacheKey> T getKey(Class<T> cacheKeyType) {
							return (T) key;
						}
					}, rr);
				}
			}
		}
	}

	public void setApplicationCache(ApplicationCache applicationCache) {
		this.applicationCache = applicationCache;
	}

	public void setEntityCache(EntityCache entityCache) {
		this.entityCache = entityCache;
	}

	public void setRequestCache(RequestCache requestCache) {
		this.requestCache = requestCache;
	}

	public void setCacheLocaleProvider(CacheLocaleProvider cacheLocaleProvider) {
		this.cacheLocaleProvider = cacheLocaleProvider;
	}

}
