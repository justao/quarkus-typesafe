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

package org.quarkusverse.typesafe.it;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import org.springaicommunity.typesafe.RetryPolicy;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.TypeSafeModels;
import org.springaicommunity.typesafe.api.TypeSafeApi;

/**
 * The "an application defines its own client" half of the back-off test.
 *
 * <p>
 * It is a class rather than a producer method on purpose: {@code @DefaultBean} backs off from
 * another bean <em>of the client's own type</em>, and a subclass is the only way for an
 * application to contribute such a bean without going through the extension's producer. It is
 * an unselected {@code @Alternative}, so it is invisible to every test that does not ask for
 * it by name — which is what {@code CustomClientProfile} does.
 *
 * <p>
 * The API root is never contacted: the point of the test is which bean wins, not what it can
 * do.
 */
@Alternative
@Singleton
public class CustomTypeSafeClient extends TypeSafeClient {

	public CustomTypeSafeClient() {
		super(TypeSafeApi.builder().baseUrl("http://localhost:1").apiKey("hand-rolled").build(),
				TypeSafeModels.JEV_PREVIEW, RetryPolicy.defaults());
	}

}
