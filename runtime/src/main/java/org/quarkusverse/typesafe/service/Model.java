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
 * Pins the model of one method, overriding the model the service or the client declares.
 *
 * <pre>{@code
 * @Model("jev-1.13.0")
 * @Noul("Is this a sentence?")
 * double sentenceProbability(String message);
 * }</pre>
 *
 * <p>
 * It exists for the case where one method of a service must not move underneath the
 * application — a regression test, or an evaluation that has to stay comparable — while the
 * rest of the service follows the configured alias.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Model {

	/**
	 * @return the model name or alias this method is answered with
	 */
	String value();

}
