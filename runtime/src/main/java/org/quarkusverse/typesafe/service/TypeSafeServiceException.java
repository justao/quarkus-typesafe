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

import org.springaicommunity.typesafe.exception.TypeSafeException;

/**
 * A failure of the declarative layer itself: a state that is missing, a question name
 * declared twice, a method shape the build should have rejected.
 *
 * <p>
 * It extends {@link TypeSafeException} on purpose. The SDK's own failures — a rate limit, a
 * connection error, an answer of the wrong kind — are thrown unchanged, so a caller can keep
 * catching the typed SDK exceptions it already catches; this is only for problems that arise
 * from what the interface declared.
 */
public class TypeSafeServiceException extends TypeSafeException {

	public TypeSafeServiceException(String message) {
		super(message);
	}

	public TypeSafeServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
