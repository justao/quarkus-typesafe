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

import org.quarkusverse.typesafe.service.Choice;
import org.quarkusverse.typesafe.service.Noul;
import org.quarkusverse.typesafe.service.Option;
import org.quarkusverse.typesafe.service.Score;

/**
 * The structured answer of one call: three questions, one state, one round trip.
 *
 * <p>
 * The components carry the questions, so the answer names are the component names and the
 * method that returns this record declares nothing of its own.
 *
 * @param urgent whether the ticket is urgent
 * @param team which team should handle it
 * @param level how frustrated the customer is
 */
public record TriageReport(@Noul("Does this convey urgency?") boolean urgent,
		@Choice(value = "Which team should handle this?",
				options = { @Option(name = "billing", criteria = "Payments, invoicing, refunds"),
						@Option(name = "technical", criteria = "Bugs, outages, integrations") }) String team,
		@Score(value = "How frustrated is the customer?", levels = { "Calm", "Frustrated", "Very angry" }) int level) {
}
