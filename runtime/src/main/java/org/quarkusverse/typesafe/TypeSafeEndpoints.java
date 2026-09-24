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

package org.quarkusverse.typesafe;

/**
 * The API paths in use, exposed so an application can see what the client talks to without
 * reaching into the SDK — for a health check or for observability, for example.
 *
 * <p>
 * This is the Quarkus counterpart of the reference starter's nested
 * {@code TypeSafeAutoConfiguration.TypeSafeEndpoints} record, made a top-level type because
 * there is no auto-configuration class to nest it in.
 *
 * @param systemOnePath the evaluation endpoint path
 * @param modelsPath the model listing endpoint path
 */
public record TypeSafeEndpoints(String systemOnePath, String modelsPath) {
}
