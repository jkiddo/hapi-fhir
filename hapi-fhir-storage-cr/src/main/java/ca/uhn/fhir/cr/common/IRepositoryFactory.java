/*-
 * #%L
 * HAPI FHIR - Clinical Reasoning
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.cr.common;

import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.google.common.annotations.Beta;

@Deprecated(since = "8.1.4", forRemoval = true)
@FunctionalInterface
@Beta
public interface IRepositoryFactory {
	HapiFhirRepository create(RequestDetails theRequestDetails);
}
