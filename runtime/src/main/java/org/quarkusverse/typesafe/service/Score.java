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
 * Asks a score question: where the state sits on an ordered rubric.
 *
 * <pre>{@code
 * @Score(value = "How frustrated is the customer?",
 *        levels = { "Calm", "Frustrated", "Very angry" })
 * int frustration(String message);
 * }</pre>
 *
 * <p>
 * The answer is continuous — a {@code 1.6} on a three level rubric sits between the second
 * and third level rather than rounding to either. What a method gets back depends on what it
 * declares: an integral return gets the level carrying the most probability
 * ({@code nearestLevel()}), a {@code double} gets the weighted value, a {@code String} gets
 * the nearest level's description and an enum gets the constant at that level's index.
 *
 * <p>
 * <strong>The simple name collides with the SDK's {@code Score} question type</strong>; see
 * {@link Noul} for what that means for imports.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Scores.class)
@Target({ ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD })
public @interface Score {

	/**
	 * What the model is being asked.
	 * @return the instructions, or an empty string when the levels carry the question
	 */
	String value() default "";

	/**
	 * The name the answer will carry.
	 * @return the answer key, or an empty string to use the method or record component name
	 */
	String name() default "";

	/**
	 * The level descriptions, ordered from the lowest score upwards. Level {@code 0} is the
	 * first entry.
	 * @return at least two levels
	 */
	String[] levels() default {};

}
