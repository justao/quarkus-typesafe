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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Asks a choice question: the answer is one of the declared {@link Option}s.
 *
 * <pre>{@code
 * @Choice(value = "Which team should handle this?",
 *         options = { @Option(name = "billing", criteria = "Payments, invoicing, refunds"),
 *                     @Option(name = "technical", criteria = "Bugs, outages, integrations") })
 * String department(String message);
 * }</pre>
 *
 * <p>
 * A method returning an enum gets the option whose name matches the selected label, and a
 * method returning a {@code String} gets the label itself.
 *
 * <p>
 * <strong>The simple name collides with the SDK's {@code Choice} question type</strong>; see
 * {@link Noul} for what that means for imports.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Choices.class)
@Target({ ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD })
public @interface Choice {

	/**
	 * What the model is being asked.
	 * @return the instructions, or an empty string to let the options speak for themselves
	 */
	String value() default "";

	/**
	 * The name the answer will carry.
	 * @return the answer key, or an empty string to use the method or record component name
	 */
	String name() default "";

	/**
	 * @return the options, at least two, in the order they should be presented
	 */
	Option[] options() default {};

}
