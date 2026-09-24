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
 * Asks a binary truth question (a Jev <em>noul</em>): the answer is a value between
 * {@code 0} and {@code 1}.
 *
 * <pre>{@code
 * @Noul("Does this convey urgency?")
 * boolean urgent(String message);
 *
 * @Noul(value = "Is the customer asking for money back?", name = "refund",
 *       whenTrue = "Explicitly asks for a refund", whenFalse = "No refund requested")
 * boolean refundRequested(String message);
 * }</pre>
 *
 * <p>
 * The annotation is repeatable, so several nouls can be asked in one call when the method
 * returns a {@code SystemOneResponse}.
 *
 * <p>
 * <strong>The simple name collides with the SDK's {@code Noul} question type.</strong> Java
 * does not allow two single-type imports of the same name, so a file that also builds
 * questions by hand has to spell one of them out in full.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Nouls.class)
@Target({ ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD })
public @interface Noul {

	/**
	 * What the model is being asked, in plain text.
	 * @return the instructions, or an empty string when the criteria carry the question
	 */
	String value() default "";

	/**
	 * The name the answer will carry.
	 * @return the answer key, or an empty string to use the method or record component name
	 */
	String name() default "";

	/**
	 * Describes what counts as true, which sharpens the boundary the model has to draw.
	 * @return the description of the true outcome, or an empty string to omit it
	 */
	String whenTrue() default "";

	/**
	 * @return the description of the false outcome, or an empty string to omit it
	 */
	String whenFalse() default "";

	/**
	 * The lower bound, inclusive, above which a {@code boolean} return counts as true. It
	 * is only meaningful for a {@code boolean} or {@code Boolean} return; declaring it
	 * anywhere else fails the build rather than being silently ignored.
	 * @return the truth threshold, between {@code 0} and {@code 1}
	 */
	double threshold() default 0.5d;

}
