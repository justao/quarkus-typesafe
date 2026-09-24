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

package org.quarkusverse.typesafe.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a TypeSafe service: every abstract method of it becomes one Jev
 * call, answered against the state the method was given.
 *
 * <pre>{@code
 * @RegisterTypeSafeService
 * public interface TriageService {
 *
 *     @Noul("Does this convey urgency?")
 *     boolean urgent(String message);
 * }
 *
 * @Inject
 * TriageService triage;
 * }</pre>
 *
 * <p>
 * There is no implementation to write. The extension produces a bean for the interface at
 * build time, so an application that has not been given an API key still starts and only
 * fails when a service method is actually called — the same promise the
 * {@code TypeSafeClient} producer makes.
 *
 * <p>
 * What a method may look like, and which return types are supported, is checked while the
 * application is built: an unattributed method, a question whose kind does not match the
 * return type, or a record component without a question fails the build with a message
 * naming the interface and the method.
 *
 * <p>
 * A method may instead return a record whose components each carry a question annotation,
 * in which case one call answers all of them:
 *
 * <pre>{@code
 * record Triage(@Noul("Is it urgent?") boolean urgent,
 *               @Choice(value = "Which team?", options = @Option(name = "billing")) String team) {
 * }
 * }</pre>
 *
 * <p>
 * <strong>Do not add a CDI scope annotation to the interface.</strong> The extension
 * registers the service as a {@code @Singleton} itself; an interface carrying
 * {@code @ApplicationScoped} is rejected at build time, because ArC would then look for an
 * implementation that does not exist.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterTypeSafeService {

	/**
	 * Model name or alias every method of this service uses, unless the method carries its
	 * own {@link Model}.
	 * @return the model, or an empty string to use the client's default model
	 */
	String model() default "";

}
