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
 * Marks the parameter carrying questions the annotations cannot express.
 *
 * <pre>{@code
 * @Noul("Is it urgent?")
 * SystemOneResponse triage(String message, @Questions Map<String, ? extends Question> extra);
 * }</pre>
 *
 * <p>
 * The entries are merged into the questions the method declares, in declaration order, which
 * is the escape hatch for a question whose instructions or criteria are structured rather
 * than plain text: build those with the SDK's own builders and pass them here.
 *
 * <p>
 * A name declared both by an annotation and by this map is an error rather than a silent
 * override — two questions with one name would make the response ambiguous.
 *
 * <p>
 * It is only meaningful on a method returning a {@code SystemOneResponse}, since that is the
 * only return type that can carry an answer the method did not declare; the build rejects it
 * anywhere else.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Questions {

}
