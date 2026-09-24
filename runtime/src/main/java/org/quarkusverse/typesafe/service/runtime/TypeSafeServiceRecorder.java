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

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Tells the build-time step how to create a service bean at run time.
 *
 * <p>
 * The recorder is the only bridge between the two halves of an extension: the deployment
 * module runs while the application is built, this class runs when it starts, and what
 * crosses between them is what a recorder method returns — recorded as bytecode. Only the
 * interface's name crosses, so nothing about the interface has to be reachable from the
 * build JVM.
 *
 * <p>
 * The returned supplier does all its work on first use, so recording it at static init costs
 * nothing and an application that never calls a service never loads anything.
 */
@Recorder
public class TypeSafeServiceRecorder {

	/**
	 * @param interfaceName the binary name of the service interface
	 * @return the supplier the synthetic bean uses to create the proxy
	 */
	public Supplier<Object> typeSafeService(String interfaceName) {
		return new TypeSafeServiceSupplier(interfaceName);
	}

}
