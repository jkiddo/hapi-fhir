package ca.uhn.fhir.jpa.dao.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.test.BaseJpaDstu3Test;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.util.HapiExtensions;
import org.apache.commons.text.RandomStringGenerator;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings({"Duplicates"})
public class FhirResourceDaoDstu3SourceTest extends BaseJpaDstu3Test {

	@AfterEach
	public final void after() {
		when(mySrd.getRequestId()).thenReturn(null);
		myStorageSettings.setStoreMetaSourceInformation(new JpaStorageSettings().getStoreMetaSourceInformation());
	}

	@BeforeEach
	public void before() {
		myStorageSettings.setStoreMetaSourceInformation(JpaStorageSettings.StoreMetaSourceInformationEnum.SOURCE_URI_AND_REQUEST_ID);
	}

	@Test
	public void testSourceStoreAndSearch() {
		String requestId = "a_request_id";

		when(mySrd.getRequestId()).thenReturn(requestId);
		Patient pt0 = new Patient();
		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:0"));
		pt0.setActive(true);
		IIdType pt0id = myPatientDao.create(pt0, mySrd).getId().toUnqualifiedVersionless();

		Patient pt1 = new Patient();
		pt1.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:1"));
		pt1.setActive(true);
		IIdType pt1id = myPatientDao.create(pt1, mySrd).getId().toUnqualifiedVersionless();

		// Search by source URI
		SearchParameterMap params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenParam("urn:source:0"));
		IBundleProvider result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue());
		pt0 = (Patient) result.getResources(0, 1).get(0);
		assertEquals("urn:source:0#a_request_id", pt0.getMeta().getExtensionString(HapiExtensions.EXT_META_SOURCE));

		// Search by request ID
		params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenParam("#a_request_id"));
		result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue(), pt1id.getValue());

		// Search by source URI and request ID
		params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenParam("urn:source:0#a_request_id"));
		result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue());

	}


	@Test
	public void testSearchWithOr() {
		String requestId = "a_request_id";

		when(mySrd.getRequestId()).thenReturn(requestId);
		Patient pt0 = new Patient();
		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:0"));
		pt0.setActive(true);
		IIdType pt0id = myPatientDao.create(pt0, mySrd).getId().toUnqualifiedVersionless();

		Patient pt1 = new Patient();
		pt1.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:1"));
		pt1.setActive(true);
		IIdType pt1id = myPatientDao.create(pt1, mySrd).getId().toUnqualifiedVersionless();

		Patient pt2 = new Patient();
		pt2.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:2"));
		pt2.setActive(true);
		myPatientDao.create(pt2, mySrd).getId().toUnqualifiedVersionless();

		// Search
		SearchParameterMap params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenOrListParam()
			.addOr(new TokenParam("urn:source:0"))
			.addOr(new TokenParam("urn:source:1")));
		IBundleProvider result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue(), pt1id.getValue());

	}

	@Test
	public void testSearchWithAnd() {
		String requestId = "a_request_id";

		when(mySrd.getRequestId()).thenReturn(requestId);
		Patient pt0 = new Patient();
		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:0"));
		pt0.setActive(true);
		IIdType pt0id = myPatientDao.create(pt0, mySrd).getId().toUnqualifiedVersionless();

		Patient pt1 = new Patient();
		pt1.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:1"));
		pt1.setActive(true);
		IIdType pt1id = myPatientDao.create(pt1, mySrd).getId().toUnqualifiedVersionless();

		Patient pt2 = new Patient();
		pt2.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:2"));
		pt2.setActive(true);
		myPatientDao.create(pt2, mySrd).getId().toUnqualifiedVersionless();

		// Search
		SearchParameterMap params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenAndListParam()
			.addAnd(new TokenParam("urn:source:0"), new TokenParam("@a_request_id")));
		IBundleProvider result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue());

	}

	@Test
	public void testSearchLongRequestId() {
		String requestId = new RandomStringGenerator.Builder().build().generate(5000);
		when(mySrd.getRequestId()).thenReturn(requestId);

		Patient pt0 = new Patient();
		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:0"));
		pt0.setActive(true);
		IIdType pt0id = myPatientDao.create(pt0, mySrd).getId().toUnqualifiedVersionless();

		// Search
		SearchParameterMap params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenAndListParam()
			.addAnd(new TokenParam("urn:source:0"), new TokenParam("#" + requestId)));
		IBundleProvider result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue());

	}

	@Test
	public void testSourceNotPreservedAcrossUpdate() {

		Patient pt0 = new Patient();
		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:0"));
		pt0.setActive(true);
		IIdType pt0id = myPatientDao.create(pt0, mySrd).getId().toUnqualifiedVersionless();

		pt0 = myPatientDao.read(pt0id);
		assertEquals("urn:source:0", pt0.getMeta().getExtensionString(HapiExtensions.EXT_META_SOURCE));

		pt0.getMeta().getExtension().clear();
		pt0.setActive(false);
		myPatientDao.update(pt0);

		pt0 = myPatientDao.read(pt0id.withVersion("2"));
		assertNull(pt0.getMeta().getExtensionString(HapiExtensions.EXT_META_SOURCE));

	}

	@Test
	public void testSourceDisabled() {
		myStorageSettings.setStoreMetaSourceInformation(JpaStorageSettings.StoreMetaSourceInformationEnum.NONE);
		when(mySrd.getRequestId()).thenReturn("0000000000000000");

		Patient pt0 = new Patient();
		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:0"));
		pt0.setActive(true);
		IIdType pt0id = myPatientDao.create(pt0, mySrd).getId().toUnqualifiedVersionless();

		pt0 = myPatientDao.read(pt0id);
		assertNull(pt0.getMeta().getExtensionString(HapiExtensions.EXT_META_SOURCE));

		pt0.getMeta().addExtension(HapiExtensions.EXT_META_SOURCE, new StringType("urn:source:1"));
		pt0.setActive(false);
		myPatientDao.update(pt0);

		pt0 = myPatientDao.read(pt0id.withVersion("2"));
		assertNull(pt0.getMeta().getExtensionString(HapiExtensions.EXT_META_SOURCE));

		// Search without source param
		SearchParameterMap params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		IBundleProvider result = myPatientDao.search(params);
		assertThat(toUnqualifiedVersionlessIdValues(result)).containsExactlyInAnyOrder(pt0id.getValue());

		// Search with source param
		 params = new SearchParameterMap();
		params.setLoadSynchronous(true);
		params.add(Constants.PARAM_SOURCE, new TokenAndListParam()
			.addAnd(new TokenParam("urn:source:0"), new TokenParam("@a_request_id")));
		try {
			myPatientDao.search(params);
		} catch (InvalidRequestException e) {
			assertEquals(e.getMessage(), Msg.code(1216) + "The _source parameter is disabled on this server");
		}
	}

	public static void assertConflictException(String theResourceType, ResourceVersionConflictException e) {
		assertThat(e.getMessage()).matches("Unable to delete [a-zA-Z]+/[0-9]+ because at least one resource has a reference to this resource. First reference found was resource " + theResourceType + "/[0-9]+ in path [a-zA-Z]+.[a-zA-Z]+");

	}

}
