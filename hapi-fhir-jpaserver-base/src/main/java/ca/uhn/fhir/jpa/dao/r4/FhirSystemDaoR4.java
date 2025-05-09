/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.jpa.dao.BaseHapiFhirSystemDao;
import ca.uhn.fhir.jpa.dao.JpaResourceDao;
import ca.uhn.fhir.jpa.model.entity.TagDefinition;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import jakarta.persistence.TypedQuery;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;

import java.util.Collection;
import java.util.List;

public class FhirSystemDaoR4 extends BaseHapiFhirSystemDao<Bundle, Meta> {

	@Override
	public Meta metaGetOperation(RequestDetails theRequestDetails) {
		String sql = "SELECT d FROM TagDefinition d WHERE d.myId IN (SELECT DISTINCT t.myTagId FROM ResourceTag t)";
		TypedQuery<TagDefinition> q = myEntityManager.createQuery(sql, TagDefinition.class);
		List<TagDefinition> tagDefinitions = q.getResultList();

		return toMeta(tagDefinitions);
	}

	@Override
	public IBaseBundle processMessage(RequestDetails theRequestDetails, IBaseBundle theMessage) {
		return JpaResourceDao.throwProcessMessageNotImplemented();
	}

	protected Meta toMeta(Collection<TagDefinition> tagDefinitions) {
		Meta retVal = new Meta();
		for (TagDefinition next : tagDefinitions) {
			switch (next.getTagType()) {
				case PROFILE:
					retVal.addProfile(next.getCode());
					break;
				case SECURITY_LABEL:
					retVal.addSecurity()
							.setSystem(next.getSystem())
							.setCode(next.getCode())
							.setDisplay(next.getDisplay());
					break;
				case TAG:
					retVal.addTag()
							.setSystem(next.getSystem())
							.setCode(next.getCode())
							.setDisplay(next.getDisplay());
					break;
			}
		}
		return retVal;
	}
}
