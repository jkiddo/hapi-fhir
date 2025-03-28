/*-
 * #%L
 * HAPI FHIR JPA Model
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
package ca.uhn.fhir.jpa.model.dialect;

import ca.uhn.fhir.jpa.migrate.DriverTypeEnum;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Dialect for H2 database.
 * Minimum version: 2.2.220
 */
public class HapiFhirH2Dialect extends H2Dialect implements IHapiFhirDialect {

	public HapiFhirH2Dialect() {
		super();
	}

	public HapiFhirH2Dialect(DialectResolutionInfo info) {
		super(info);
	}

	/**
	 * As of Hibernate 6, generated schemas include a column level check constraint that enforces valid values
	 * for columns that back an Enum type. For example, the column definition for <code>ResourceTable#getFhirVersion()</code>
	 * would look like:
	 * <pre>
	 *  RES_VERSION varchar(7) check (RES_VERSION in ('DSTU2','DSTU2_HL7ORG','DSTU2_1','DSTU3','R4','R4B','R5')),
	 * </pre>
	 * <p>
	 * This is a nice addition since it enforces the values that the Enum allows, but it's problematic for us because these
	 * constraints are invisible to the JDBC metadata API on most databases, which means that our schema migration
	 * checker isn't able to catch problems if we add a value to an Enum and don't add a corresponding database
	 * migration. Rather than risk having inconsistent behaviour between annotated and migrated schemas, we just
	 * disable these checks on all of our dialects.
	 * </p><p>
	 * See this discussion from the author of SchemaCrawler discussing this limitation:
	 * <a href="https://stackoverflow.com/questions/63346650/schemacrawler-java-api-retrieve-check-column-constraints">https://stackoverflow.com/questions/63346650/schemacrawler-java-api-retrieve-check-column-constraints</a>.
	 * With this change in place, the definition above becomes simply:
	 * <pre>
	 *  RES_VERSION varchar(7),
	 * </pre>
	 * </p>
	 * <p>
	 * Also note, as of Hibernate 6.6, Enum types are also used on some database platforms,
	 * creating a definition like:
	 * <code>STAT enum ('CANCELLED','COMPLETED','ERRORED','FAILED','FINALIZE','IN_PROGRESS','QUEUED') not null</code>.
	 * We also disable these by overriding <code>getEnumTypeDeclaration</code>.
	 * </p>
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public DriverTypeEnum getDriverType() {
		return DriverTypeEnum.H2_EMBEDDED;
	}

	/**
	 * @see HapiFhirH2Dialect#supportsColumnCheck() for an explanation of why we disable this
	 */
	@Override
	public String getEnumTypeDeclaration(Class<? extends Enum<?>> enumType) {
		return null;
	}

	/**
	 * @see HapiFhirH2Dialect#supportsColumnCheck() for an explanation of why we disable this
	 */
	@Override
	public String getEnumTypeDeclaration(String name, String[] values) {
		return null;
	}
}
