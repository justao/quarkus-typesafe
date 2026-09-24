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

package org.quarkusverse.typesafe.it.service;

import org.quarkusverse.typesafe.service.Noul;
import org.quarkusverse.typesafe.service.RegisterTypeSafeService;

/**
 * A service whose model is pinned on the interface rather than on a method, so a test can
 * tell the two levels of the model override apart.
 */
@RegisterTypeSafeService(model = "jev-preview")
public interface PinnedService {

	/**
	 * @param message the content to evaluate
	 * @return whether the message is a sentence
	 */
	@Noul("Is this a sentence?")
	boolean sentence(String message);

}
