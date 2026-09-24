/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quarkusverse.typesafe.service.runtime;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import org.quarkusverse.typesafe.service.TypeSafeServiceException;

/**
 * Creates the proxy that implements a service interface, once, on first use.
 *
 * <p>
 * The class is public and has exactly one constructor because the build step records its
 * construction: the deployment module knows the interface's name, not the interface, so the
 * name is all that is recorded and the interface is loaded here, at run time.
 *
 * <p>
 * For a native image the proxy needs to be registered at build time, which the same build
 * step does; nothing here is reflection over the application except that registration.
 */
public class TypeSafeServiceSupplier implements Supplier<Object> {

	private final String interfaceName;

	private volatile Object proxy;

	/**
	 * @param interfaceName the binary name of the service interface
	 */
	public TypeSafeServiceSupplier(String interfaceName) {
		if (interfaceName == null || interfaceName.isEmpty()) {
			throw new IllegalArgumentException("interfaceName must not be empty");
		}
		this.interfaceName = interfaceName;
	}

	@Override
	public Object get() {
		Object existing = this.proxy;
		if (existing == null) {
			synchronized (this) {
				existing = this.proxy;
				if (existing == null) {
					existing = Proxy.newProxyInstance(classLoader(), new Class<?>[] { load() },
							new TypeSafeServiceInvocationHandler(this.interfaceName));
					this.proxy = existing;
				}
			}
		}
		return existing;
	}

	private Class<?> load() {
		try {
			return Class.forName(this.interfaceName, false, classLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new TypeSafeServiceException("Cannot load the service interface " + this.interfaceName, ex);
		}
	}

	private ClassLoader classLoader() {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return loader != null ? loader : TypeSafeServiceSupplier.class.getClassLoader();
	}

}
