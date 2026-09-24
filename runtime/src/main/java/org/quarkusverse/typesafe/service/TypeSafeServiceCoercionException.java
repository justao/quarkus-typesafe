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

/**
 * The answer arrived but cannot be turned into what the method declared — an enum with no
 * constant of that name, a level index past the end of the enum, a value out of range for
 * the declared number type.
 *
 * <p>
 * This is the gap the build cannot close. The build knows the question kind and the return
 * type and refuses combinations that can never work; whether the model picks a label an enum
 * happens to declare is only visible once the answer is in.
 */
public class TypeSafeServiceCoercionException extends TypeSafeServiceException {

	private final String method;

	private final String questionName;

	/**
	 * @param method the service method, as {@code Interface.method}
	 * @param questionName the answer key the value came from
	 * @param message what could not be converted, and what would have been accepted
	 */
	public TypeSafeServiceCoercionException(String method, String questionName, String message) {
		super(message + " (method " + method + ", answer '" + questionName + "')");
		this.method = method;
		this.questionName = questionName;
	}

	/**
	 * @param method the service method, as {@code Interface.method}
	 * @param questionName the answer key the value came from
	 * @param message what could not be converted, and what would have been accepted
	 * @param cause the conversion failure
	 */
	public TypeSafeServiceCoercionException(String method, String questionName, String message, Throwable cause) {
		super(message + " (method " + method + ", answer '" + questionName + "')", cause);
		this.method = method;
		this.questionName = questionName;
	}

	/**
	 * @return the service method the failure belongs to
	 */
	public String method() {
		return this.method;
	}

	/**
	 * @return the answer key the value came from
	 */
	public String questionName() {
		return this.questionName;
	}

}
