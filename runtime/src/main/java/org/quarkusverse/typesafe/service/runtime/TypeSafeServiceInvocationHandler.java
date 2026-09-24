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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import org.quarkusverse.typesafe.service.TypeSafeServiceException;
import org.springaicommunity.typesafe.TypeSafeClient;

/**
 * Answers a service interface's methods by asking Jev.
 *
 * <p>
 * The client is looked up on the first call rather than when the bean is created, which is
 * what keeps the promise the {@code TypeSafeClient} producer makes: an application with no
 * API key starts, and only a call that actually needs the client fails — with the
 * configuration error naming the property to set.
 *
 * <p>
 * The per-method plans are cached, so the annotations are read once per method rather than
 * once per call, and the handler holds no state that a concurrent call could corrupt.
 */
final class TypeSafeServiceInvocationHandler implements InvocationHandler {

	private static final Object[] NO_ARGS = new Object[0];

	private final String interfaceName;

	private final ConcurrentHashMap<Method, ServiceMethodPlan> plans = new ConcurrentHashMap<>();

	private volatile TypeSafeClient client;

	TypeSafeServiceInvocationHandler(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass() == Object.class) {
			return objectMethod(proxy, method, args);
		}
		if (method.isDefault()) {
			// A default method is the interface's own code, and the place to put anything
			// that should not cost a call.
			return InvocationHandler.invokeDefault(proxy, method, args);
		}
		ServiceMethodPlan plan = this.plans.computeIfAbsent(method, m -> ServiceMethodPlan.of(m, this.interfaceName));
		return plan.invoke(client(), args == null ? NO_ARGS : args);
	}

	private Object objectMethod(Object proxy, Method method, Object[] args) throws Throwable {
		return switch (method.getName()) {
			case "toString" -> this.interfaceName + " service proxy";
			case "hashCode" -> System.identityHashCode(proxy);
			case "equals" -> proxy == args[0];
			default -> method.invoke(this, args);
		};
	}

	private TypeSafeClient client() {
		TypeSafeClient resolved = this.client;
		if (resolved == null) {
			ArcContainer container = Arc.container();
			if (container == null) {
				throw new TypeSafeServiceException(
						"ArC is not running, so " + this.interfaceName + " cannot look up its TypeSafeClient");
			}
			InstanceHandle<TypeSafeClient> handle = container.instance(TypeSafeClient.class);
			if (!handle.isAvailable()) {
				throw new TypeSafeServiceException("No TypeSafeClient bean is available for " + this.interfaceName
						+ "; declare one, or configure quarkus.typesafe.api-key so the extension can produce one");
			}
			resolved = handle.get();
			this.client = resolved;
		}
		return resolved;
	}

}
