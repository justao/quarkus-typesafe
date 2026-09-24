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

package org.quarkusverse.typesafe.deployment;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;

/**
 * One validated {@code @RegisterTypeSafeService} interface, on its way from the step that
 * checked it to the step that registers its bean.
 *
 * @param interfaceName the interface
 * @param reflectionTypes the types the runtime reflects on: the records it constructs and the
 * enums it enumerates
 */
public final class TypeSafeServiceBuildItem extends MultiBuildItem {

	private final DotName interfaceName;

	private final Set<DotName> reflectionTypes;

	/**
	 * @param interfaceName the interface
	 * @param reflectionTypes the types to register for reflection
	 */
	public TypeSafeServiceBuildItem(DotName interfaceName, Set<DotName> reflectionTypes) {
		this.interfaceName = interfaceName;
		this.reflectionTypes = Collections.unmodifiableSet(new LinkedHashSet<>(reflectionTypes));
	}

	/**
	 * @return the service interface's name
	 */
	public DotName interfaceName() {
		return this.interfaceName;
	}

	/**
	 * @return the types this service needs reflection on
	 */
	public Set<DotName> reflectionTypes() {
		return this.reflectionTypes;
	}

}
