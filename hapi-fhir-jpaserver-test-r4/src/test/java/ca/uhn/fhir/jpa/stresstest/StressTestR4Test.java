package ca.uhn.fhir.jpa.stresstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import ca.uhn.fhir.batch2.model.StatusEnum;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.model.DeleteMethodOutcome;
import ca.uhn.fhir.jpa.api.svc.ISearchCoordinatorSvc;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.provider.BaseResourceProviderR4Test;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.SearchCoordinatorSvcImpl;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.test.config.TestR4Config;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import ca.uhn.fhir.util.StopWatch;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.hapi.rest.server.helper.BatchHelperR4;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
	"max_db_connections=10"
})
@DirtiesContext
public class StressTestR4Test extends BaseResourceProviderR4Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(StressTestR4Test.class);

	static {
		TestR4Config.ourMaxThreads = 10;
	}

	private RequestValidatingInterceptor myRequestValidatingInterceptor;
	@Autowired
	private DatabaseBackedPagingProvider myPagingProvider;
	private int myPreviousMaxPageSize;
	@Autowired
	private ISearchCoordinatorSvc mySearchCoordinatorSvc;

	@Override
	@AfterEach
	public void after() throws Exception {
		super.after();

		myServer.unregisterInterceptor(myRequestValidatingInterceptor);
		myStorageSettings.setIndexMissingFields(JpaStorageSettings.IndexEnabledEnum.ENABLED);

		myPagingProvider.setMaximumPageSize(myPreviousMaxPageSize);

		SearchCoordinatorSvcImpl searchCoordinator = AopTestUtils.getTargetObject(mySearchCoordinatorSvc);
		searchCoordinator.setLoadingThrottleForUnitTests(null);
		myStorageSettings.setSearchPreFetchThresholds(new JpaStorageSettings().getSearchPreFetchThresholds());

	}

	@Override
	@BeforeEach
	public void before() throws Exception {
		super.before();

		myRequestValidatingInterceptor = new RequestValidatingInterceptor();
		FhirInstanceValidator module = new FhirInstanceValidator(myFhirContext);
		module.setValidationSupport(myValidationSupport);
		myRequestValidatingInterceptor.addValidatorModule(module);

		myPreviousMaxPageSize = myPagingProvider.getMaximumPageSize();
		myPagingProvider.setMaximumPageSize(300);
	}

	@Disabled("Stress test")
	@Test
	public void testNoDuplicatesInSearchResults() throws Exception {
		int resourceCount = 1000;
		int queryCount = 30;
		myStorageSettings.setSearchPreFetchThresholds(Lists.newArrayList(50, 200, -1));

		SearchCoordinatorSvcImpl searchCoordinator = AopTestUtils.getTargetObject(mySearchCoordinatorSvc);
		searchCoordinator.setLoadingThrottleForUnitTests(10);

		Bundle bundle = new Bundle();

		for (int i = 0; i < resourceCount; i++) {
			Observation o = new Observation();
			o.setId("A" + leftPad(Integer.toString(i), 4, '0'));
			o.setEffective(DateTimeType.now());
			o.setStatus(Observation.ObservationStatus.FINAL);
			bundle.addEntry().setFullUrl(o.getId()).setResource(o).getRequest().setMethod(HTTPVerb.PUT).setUrl("Observation/A" + i);
		}
		StopWatch sw = new StopWatch();
		ourLog.info("Saving {} resources", bundle.getEntry().size());
		mySystemDao.transaction(null, bundle);
		ourLog.info("Saved {} resources in {}", bundle.getEntry().size(), sw);

		Map<String, IBaseResource> ids = new HashMap<>();

		IGenericClient fhirClient = this.myClient;

		String url = myServerBase + "/Observation?date=gt2000&_sort=-_lastUpdated";

		int pageIndex = 0;
		ourLog.info("Loading page {}", pageIndex);
		Bundle searchResult = fhirClient
			.search()
			.byUrl(url)
			.count(queryCount)
			.returnBundle(Bundle.class)
			.execute();
		while (true) {
			List<String> passIds = searchResult
				.getEntry()
				.stream()
				.map(t -> t.getResource().getIdElement().getValue())
				.toList();

			int index = 0;
			for (String nextId : passIds) {
				Resource nextResource = searchResult.getEntry().get(index).getResource();

				if (ids.containsKey(nextId)) {
					String previousContent = fhirClient.getFhirContext().newJsonParser().encodeResourceToString(ids.get(nextId));
					String newContent = fhirClient.getFhirContext().newJsonParser().encodeResourceToString(nextResource);
					throw new Exception("Duplicate ID " + nextId + " found at index " + index + " of page " + pageIndex + "\n\nPrevious: " + previousContent + "\n\nNew: " + newContent);
				}
				ids.put(nextId, nextResource);
				index++;
			}

			if (searchResult.getLink(Constants.LINK_NEXT) == null) {
				break;
			} else {
				if (searchResult.getEntry().size() != queryCount) {
					throw new Exception("Page had " + searchResult.getEntry().size() + " resources");
				}
				if (passIds.size() != queryCount) {
					throw new Exception("Page had " + passIds.size() + " unique ids");
				}
			}

			pageIndex++;
			ourLog.info("Loading page {}: {}", pageIndex, searchResult.getLink(Constants.LINK_NEXT).getUrl());
			searchResult = fhirClient.loadPage().next(searchResult).execute();
		}

		assertThat(ids).hasSize(resourceCount);
	}

	@Disabled("Stress test")
	@Test
	public void testPageThroughLotsOfPages() {
		myStorageSettings.setIndexMissingFields(JpaStorageSettings.IndexEnabledEnum.DISABLED);

		/*
		 * This test creates a really huge number of resources to make sure that even large scale
		 * searches work correctly. 5000 is arbitrary, this test was intended to demonstrate an
		 * issue that occurred with 1600 resources but I'm using a huge number here just to
		 * hopefully catch future issues.
		 */
		int count = 5000;


		Bundle bundle = new Bundle();

		DiagnosticReport dr = new DiagnosticReport();
		dr.setId(IdType.newRandomUuid());
		bundle.addEntry().setFullUrl(dr.getId()).setResource(dr).getRequest().setMethod(HTTPVerb.POST).setUrl("DiagnosticReport");
		for (int i = 0; i < count; i++) {
			Observation o = new Observation();
			o.setId("A" + leftPad(Integer.toString(i), 4, '0'));
			o.setStatus(Observation.ObservationStatus.FINAL);
			bundle.addEntry().setFullUrl(o.getId()).setResource(o).getRequest().setMethod(HTTPVerb.PUT).setUrl("Observation/A" + i);
		}
		StopWatch sw = new StopWatch();
		ourLog.info("Saving {} resources", bundle.getEntry().size());
		mySystemDao.transaction(null, bundle);
		ourLog.info("Saved {} resources in {}", bundle.getEntry().size(), sw);

		// Load from DAOs
		List<String> ids = new ArrayList<>();
		Bundle resultBundle = myClient.search().forResource("Observation").count(100).returnBundle(Bundle.class).execute();
		int pageIndex = 0;
		while (true) {
			ids.addAll(resultBundle.getEntry().stream().map(t -> t.getResource().getIdElement().toUnqualifiedVersionless().getValue()).toList());
			if (resultBundle.getLink("next") == null) {
				break;
			}
			ourLog.info("Loading page {} - Have {} results: {}", pageIndex++, ids.size(), resultBundle.getLink("next").getUrl());
			resultBundle = myClient.loadPage().next(resultBundle).execute();
		}
		assertThat(ids).hasSize(count);
		assertThat(Sets.newHashSet(ids)).hasSize(count);

		// Load from DAOs
		ids = new ArrayList<>();
		SearchParameterMap map = new SearchParameterMap();
		map.add("status", new TokenOrListParam().add("final").add("aaa")); // add some noise to guarantee we don't reuse a previous query
		IBundleProvider results = myObservationDao.search(map, mySrd);
		for (int i = 0; i <= count; i += 100) {
			List<IBaseResource> resultsAndIncludes = results.getResources(i, i + 100);
			ids.addAll(toUnqualifiedVersionlessIdValues(resultsAndIncludes));
			results = myPagingProvider.retrieveResultList(null, results.getUuid());
		}
		assertThat(ids).hasSize(count);
		assertThat(Sets.newHashSet(ids)).hasSize(count);

		// Load from DAOs starting half way through
		ids = new ArrayList<>();
		map = new SearchParameterMap();
		map.add("status", new TokenOrListParam().add("final").add("aaa")); // add some noise to guarantee we don't reuse a previous query
		results = myObservationDao.search(map, mySrd);
		for (int i = 1000; i <= count; i += 100) {
			List<IBaseResource> resultsAndIncludes = results.getResources(i, i + 100);
			ids.addAll(toUnqualifiedVersionlessIdValues(resultsAndIncludes));
			results = myPagingProvider.retrieveResultList(null, results.getUuid());
		}
		assertThat(ids).hasSize(count - 1000);
		assertThat(Sets.newHashSet(ids)).hasSize(count - 1000);
	}

	@Disabled("Stress test")
	@Test
	public void testPageThroughLotsOfPages2() {
		myStorageSettings.setIndexMissingFields(JpaStorageSettings.IndexEnabledEnum.DISABLED);

		Bundle bundle = new Bundle();

		int count = 1603;
		for (int i = 0; i < count; i++) {
			Observation o = new Observation();
			o.setId("A" + leftPad(Integer.toString(i), 4, '0'));
			o.setStatus(Observation.ObservationStatus.FINAL);
			bundle.addEntry().setFullUrl(o.getId()).setResource(o).getRequest().setMethod(HTTPVerb.PUT).setUrl("Observation/A" + i);
		}
		StopWatch sw = new StopWatch();
		ourLog.info("Saving {} resources", bundle.getEntry().size());
		mySystemDao.transaction(null, bundle);
		ourLog.info("Saved {} resources in {}", bundle.getEntry().size(), sw);

		// Load from DAOs
		List<String> ids = new ArrayList<>();
		Bundle resultBundle = myClient.search().forResource("Observation").count(300).returnBundle(Bundle.class).execute();
		int pageIndex = 0;
		while (true) {
			ids.addAll(resultBundle.getEntry().stream().map(t -> t.getResource().getIdElement().toUnqualifiedVersionless().getValue()).toList());
			if (resultBundle.getLink("next") == null) {
				break;
			}
			ourLog.info("Loading page {} - Have {} results: {}", pageIndex++, ids.size(), resultBundle.getLink("next").getUrl());
			resultBundle = myClient.loadPage().next(resultBundle).execute();
		}
		assertThat(ids).hasSize(count);
		assertThat(Sets.newHashSet(ids)).hasSize(count);

	}

	@Disabled("Stress test")
	@Test
	public void testSearchWithLargeNumberOfIncludes() {

		Bundle bundle = new Bundle();

		DiagnosticReport dr = new DiagnosticReport();
		dr.setId(IdType.newRandomUuid());
		bundle.addEntry().setFullUrl(dr.getId()).setResource(dr).getRequest().setMethod(HTTPVerb.POST).setUrl("DiagnosticReport");

		for (int i = 0; i < 1200; i++) {
			Observation o = new Observation();
			o.setId(IdType.newRandomUuid());
			o.setStatus(Observation.ObservationStatus.FINAL);
			bundle.addEntry().setFullUrl(o.getId()).setResource(o).getRequest().setMethod(HTTPVerb.POST).setUrl("Observation");
			dr.addResult().setReference(o.getId());

			if (i == 0) {
				Observation o2 = new Observation();
				o2.setId(IdType.newRandomUuid());
				o2.setStatus(Observation.ObservationStatus.FINAL);
				bundle.addEntry().setFullUrl(o2.getId()).setResource(o2).getRequest().setMethod(HTTPVerb.POST).setUrl("Observation");
				o.addHasMember(new Reference(o2.getId()));
			}
		}

		StopWatch sw = new StopWatch();
		ourLog.info("Saving {} resources", bundle.getEntry().size());
		mySystemDao.transaction(null, bundle);
		ourLog.info("Saved {} resources in {}", bundle.getEntry().size(), sw);

		// Using _include=*
		SearchParameterMap map = new SearchParameterMap();
		map.addInclude(IBaseResource.INCLUDE_ALL.asRecursive());
		map.setLoadSynchronous(true);
		IBundleProvider results = myDiagnosticReportDao.search(map, mySrd);
		List<IBaseResource> resultsAndIncludes = results.getResources(0, 999999);
		assertThat(resultsAndIncludes).hasSize(1001);

		// Using focused includes
		map = new SearchParameterMap();
		map.addInclude(DiagnosticReport.INCLUDE_RESULT.asRecursive());
		map.addInclude(Observation.INCLUDE_HAS_MEMBER.asRecursive());
		map.setLoadSynchronous(true);
		results = myDiagnosticReportDao.search(map, mySrd);
		resultsAndIncludes = results.getResources(0, 999999);
		assertThat(resultsAndIncludes).hasSize(1001);
	}

	@Disabled("Stress test")
	@Test
	public void testUpdateListWithLargeNumberOfEntries() {
		int numPatients = 3000;

		ListResource lr = new ListResource();
		lr.setId(IdType.newRandomUuid());

		{
			Bundle bundle = new Bundle();
			for (int i = 0; i < numPatients; ++i) {
				Patient patient = new Patient();
				patient.setId(IdType.newRandomUuid());
				bundle.addEntry().setFullUrl(patient.getId()).setResource(patient).getRequest().setMethod(HTTPVerb.POST).setUrl("Patient");
				lr.addEntry().setItem(new Reference(patient.getId()));
			}
			bundle.addEntry().setFullUrl(lr.getId()).setResource(lr).getRequest().setMethod(HTTPVerb.POST).setUrl("List");

			StopWatch sw = new StopWatch();
			ourLog.info("Saving list with {} entries", lr.getEntry().size());
			mySystemDao.transaction(null, bundle);
			ourLog.info("Saved {} resources in {}", bundle.getEntry().size(), sw);
		}

		{
			Bundle bundle = new Bundle();

			Patient newPatient = new Patient();
			newPatient.setId(IdType.newRandomUuid());
			bundle.addEntry().setFullUrl(newPatient.getId()).setResource(newPatient).getRequest().setMethod(HTTPVerb.POST).setUrl("Patient");
			lr.addEntry().setItem(new Reference(newPatient.getId()));
			bundle.addEntry().setFullUrl(lr.getId()).setResource(lr).getRequest().setMethod(HTTPVerb.PUT).setUrl(lr.getIdElement().toUnqualifiedVersionless().getValue());

			StopWatch sw = new StopWatch();
			ourLog.info("Updating list with {} entries", lr.getEntry().size());
			mySystemDao.transaction(null, bundle);
			ourLog.info("Updated {} resources in {}", bundle.getEntry().size(), sw);
		}
	}

	@Disabled("Stress test")
	@Test
	public void testMultithreadedSearch() throws Exception {
		Bundle input = new Bundle();
		input.setType(BundleType.TRANSACTION);
		for (int i = 0; i < 500; i++) {
			Patient p = new Patient();
			p.addIdentifier().setSystem("http://test").setValue("BAR");
			input.addEntry().setResource(p).getRequest().setMethod(HTTPVerb.POST).setUrl("Patient");
		}
		myClient.transaction().withBundle(input).execute();


		List<BaseTask> tasks = Lists.newArrayList();
		try {
			for (int threadIndex = 0; threadIndex < 10; threadIndex++) {
				SearchTask task = new SearchTask();
				tasks.add(task);
				task.start();
			}
		} finally {
			for (BaseTask next : tasks) {
				next.join();
			}
		}

		validateNoErrors(tasks);

	}

	/**
	 * This tests that the SQL statement generated in DeleteExpungeSqlBuilder.findResourceLinksWithTargetPidIn()
	 * doesn't throw an error due to too many query parameters in the IN clause.
	 */
	@Disabled("Stress test")
	@Test
	public void testDeleteExpungeWithCascadingDeletesWithLargeDataSet() {
		myStorageSettings.setAllowMultipleDelete(true);
		myStorageSettings.setExpungeEnabled(true);
		myStorageSettings.setDeleteExpungeEnabled(true);
		//Given: A database with 5 patients and 100s of other resources referencing the 5 patients.
		myStorageSettings.setExpungeBatchSize(1000);
		int numOfRecords = 2505;
		IIdType p1 = createPatient(withActiveTrue());
		IIdType p2 = createPatient(withActiveTrue());
		IIdType p3 = createPatient(withActiveTrue());
		IIdType p4 = createPatient(withActiveTrue());
		IIdType p5 = createPatient(withActiveTrue());
		for (int i = 0; i < numOfRecords; ++i) {
			createObservation(withSubject(p1));
			createObservation(withSubject(p1));
			createObservation(withSubject(p1));
			createObservation(withSubject(p1));
			createObservation(withSubject(p2));
			createObservation(withSubject(p2));
			createObservation(withSubject(p2));
			createObservation(withSubject(p2));
			createObservation(withSubject(p3));
			createObservation(withSubject(p3));
			createObservation(withSubject(p3));
			createObservation(withSubject(p4));
			createObservation(withSubject(p4));
			createObservation(withSubject(p4));
			createObservation(withSubject(p5));
			createObservation(withSubject(p5));
			createObservation(withSubject(p5));
			createObservation(withSubject(p5));
			createObservation(withSubject(p5));
			createObservation(withSubject(p5));
			createObservation(withSubject(p5));
			createEncounter(withReference("subject", p1));
			createEncounter(withReference("subject", p1));
			createEncounter(withReference("subject", p1));
			createEncounter(withReference("subject", p2));
			createEncounter(withReference("subject", p2));
			createEncounter(withReference("subject", p2));
			createEncounter(withReference("subject", p3));
			createEncounter(withReference("subject", p3));
			createEncounter(withReference("subject", p3));
			createEncounter(withReference("subject", p3));
			createEncounter(withReference("subject", p4));
			createEncounter(withReference("subject", p4));
			createEncounter(withReference("subject", p4));
			createEncounter(withReference("subject", p4));
			createEncounter(withReference("subject", p5));
			createEncounter(withReference("subject", p5));
			createEncounter(withReference("subject", p5));
			createEncounter(withReference("subject", p5));
			createEncounter(withReference("subject", p5));
		}

		when(mySrd.getParameters()).thenReturn(Map.of(
			Constants.PARAMETER_CASCADE_DELETE, new String[]{Constants.CASCADE_DELETE},
			JpaConstants.PARAM_DELETE_EXPUNGE, new String[]{"true"}
		));

		//When: A delete expunge operation is initiated with cascading deletes
		DeleteMethodOutcome outcome = myPatientDao.deleteByUrl("Patient?" + JpaConstants.PARAM_DELETE_EXPUNGE + "=true", mySrd);

		//validate: Records are deleted
		String jobId = jobExecutionIdFromOutcome(outcome);
		myBatch2JobHelper.awaitJobCompletion(jobId, 120);
		assertEquals(100205, myBatch2JobHelper.getCombinedRecordsProcessed(jobId));

		JpaStorageSettings defaultStorageSettings = new JpaStorageSettings();
		myStorageSettings.setAllowMultipleDelete(defaultStorageSettings.isAllowMultipleDelete());
		myStorageSettings.setExpungeEnabled(defaultStorageSettings.isExpungeEnabled());
		myStorageSettings.setDeleteExpungeEnabled(defaultStorageSettings.isDeleteExpungeEnabled());
		myStorageSettings.setExpungeBatchSize(defaultStorageSettings.getExpungeBatchSize());
	}

	@Test
	public void testMultiThreadedCreateWithDuplicateClientAssignedIdsInTransaction() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(20);

		List<Future<String>> futures = new ArrayList<>();
		for (int i = 0; i < 100; i++) {

			int finalI = i;

			Callable<String> task = () -> {
				Bundle input = new Bundle();
				input.setType(BundleType.TRANSACTION);

				Patient p = new Patient();
				p.setId("A" + finalI);
				p.addIdentifier().setValue("A" + finalI);
				input.addEntry().setResource(p).setFullUrl("Patient/A" + finalI).getRequest().setMethod(HTTPVerb.PUT).setUrl("Patient/A" + finalI);
				Patient p2 = new Patient();
				p2.setId("B" + finalI);
				p2.addIdentifier().setValue("B" + finalI);
				input.addEntry().setResource(p2).setFullUrl("Patient/B" + finalI).getRequest().setMethod(HTTPVerb.PUT).setUrl("Patient/A" + finalI);

				try {
					myClient.transaction().withBundle(input).execute();
					return null;
				} catch (ResourceVersionConflictException e) {
					assertThat(e.toString()).contains("Error flushing transaction with resource types: [Patient (x2)] - The operation has failed with a client-assigned ID constraint failure");
					return e.toString();
				}
			};
			for (int j = 0; j < 2; j++) {
				Future<String> future = executor.submit(task);
				futures.add(future);
			}

		}

		List<String> results = new ArrayList<>();
		for (Future<String> next : futures) {
			String nextOutcome = next.get();
			if (isNotBlank(nextOutcome)) {
				results.add(nextOutcome);
			}
		}

		ourLog.info("Results: {}", results);
		assertThat(results).isNotEmpty();
		assertThat(results.get(0)).contains("HTTP 409 Conflict");
		assertThat(results.get(0)).contains("Error flushing transaction with resource types: [Patient (x2)]");
	}

	@Test
	public void testMultiThreadedUpdateSameResourceInTransaction() throws Exception {

		Patient p = new Patient();
		p.setActive(true);
		IIdType id = myPatientDao.create(p, mySrd).getId().toUnqualifiedVersionless();

		ExecutorService executor = Executors.newFixedThreadPool(20);

		List<Future<String>> futures = new ArrayList<>();
		for (int i = 0; i < 100; i++) {

			int finalI = i;

			Callable<String> task = () -> {
				Bundle input = new Bundle();
				input.setType(BundleType.TRANSACTION);

				Patient updatePatient = new Patient();
				updatePatient.setId(id);
				updatePatient.addIdentifier().setValue("A" + finalI);
				input.addEntry().setResource(updatePatient).setFullUrl(updatePatient.getId()).getRequest().setMethod(HTTPVerb.PUT).setUrl(updatePatient.getId());

				try {
					myClient.transaction().withBundle(input).execute();
					return null;
				} catch (ResourceVersionConflictException e) {
					assertThat(e.toString()).contains("Error flushing transaction with resource types: [Patient] - The operation has failed with a version constraint failure. This generally means that two clients/threads were trying to update the same resource at the same time, and this request was chosen as the failing request.");
					return e.toString();
				}
			};
			for (int j = 0; j < 2; j++) {
				Future<String> future = executor.submit(task);
				futures.add(future);
			}

		}

		List<String> results = new ArrayList<>();
		for (Future<String> next : futures) {
			String nextOutcome = next.get();
			if (isNotBlank(nextOutcome)) {
				results.add(nextOutcome);
			}
		}

		ourLog.info("Results: {}", results);
		assertThat(results).isNotEmpty();
		assertThat(results.get(0)).contains("HTTP 409 Conflict");
		assertThat(results.get(0)).contains("Error flushing transaction with resource types: [Patient]");
	}

	/**
	 * This test prevents a deadlock that was detected with a large number of
	 * threads creating resources and blocking on the searchparamcache refreshing
	 * (since this is a synchronized method) while the instance that was actually
	 * executing was waiting on a DB connection. This was solved by making
	 * JpaValidationSupportDstuXX be transactional, which it should have been
	 * anyhow.
	 */
	@Disabled("Stress test")
	@Test
	public void testMultithreadedSearchWithValidation() throws Exception {
		myServer.registerInterceptor(myRequestValidatingInterceptor);

		Bundle input = new Bundle();
		input.setType(BundleType.TRANSACTION);
		for (int i = 0; i < 500; i++) {
			Patient p = new Patient();
			p.addIdentifier().setSystem("http://test").setValue("BAR");
			input.addEntry().setResource(p).getRequest().setMethod(HTTPVerb.POST).setUrl("Patient");
		}
		myClient.transaction().withBundle(input).execute();

		try (CloseableHttpResponse getMeta = ourHttpClient.execute(new HttpGet(myServerBase + "/metadata"))) {
			assertEquals(200, getMeta.getStatusLine().getStatusCode());
		}

		List<BaseTask> tasks = Lists.newArrayList();
		try {
			for (int threadIndex = 0; threadIndex < 5; threadIndex++) {
				SearchTask task = new SearchTask();
				tasks.add(task);
				task.start();
			}
			for (int threadIndex = 0; threadIndex < 5; threadIndex++) {
				CreateTask task = new CreateTask();
				tasks.add(task);
				task.start();
			}
		} finally {
			for (BaseTask next : tasks) {
				next.join();
			}
		}

		validateNoErrors(tasks);
	}

	@Disabled("Stress test")
	@Test
	public void test_DeleteExpunge_withLargeBatchSizeManyResources() {
		// setup
		int batchSize = 1000;
		myStorageSettings.setAllowMultipleDelete(true);
		myStorageSettings.setExpungeEnabled(true);
		myStorageSettings.setDeleteExpungeEnabled(true);

		// create patients
		for (int i = 0; i < batchSize; i++) {
			Patient patient = new Patient();
			patient.setId("tracer" + i);
			patient.setActive(true);
			myClient.update().resource(patient).execute();
		}
		ourLog.info("Patients created");

		// parameters
		Parameters input = new Parameters();
		input.addParameter(ProviderConstants.OPERATION_DELETE_EXPUNGE_URL, "Patient?active=true");
		input.addParameter(ProviderConstants.OPERATION_DELETE_BATCH_SIZE, new DecimalType(batchSize));

		// execute
		myCaptureQueriesListener.clear();
		Parameters response = myClient
			.operation()
			.onServer()
			.named(ProviderConstants.OPERATION_DELETE_EXPUNGE)
			.withParameters(input)
			.execute();

		ourLog.debug(myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(response));

		String jobId = BatchHelperR4.jobIdFromBatch2Parameters(response);
		myBatch2JobHelper.awaitJobHasStatus(jobId, 60, StatusEnum.COMPLETED);
		int deleteCount = myCaptureQueriesListener.getDeleteQueries().size();

		myCaptureQueriesListener.logDeleteQueries();
		assertEquals(59, deleteCount);
	}

	@Disabled("Stress test")
	@Test
	public void testDeleteExpungeOperationOverLargeDataset() {
		myStorageSettings.setAllowMultipleDelete(true);
		myStorageSettings.setExpungeEnabled(true);
		myStorageSettings.setDeleteExpungeEnabled(true);

		// setup
		Patient patient = new Patient();
		patient.setId("tracer");
		patient.setActive(true);
		patient.getMeta().addTag().setSystem(UUID.randomUUID().toString()).setCode(UUID.randomUUID().toString());
		MethodOutcome result = myClient.update().resource(patient).execute();

		patient.setId(result.getId());
		patient.getMeta().addTag().setSystem(UUID.randomUUID().toString()).setCode(UUID.randomUUID().toString());
		myClient.update().resource(patient).execute();

		Parameters input = new Parameters();
		input.addParameter(ProviderConstants.OPERATION_DELETE_EXPUNGE_URL, "Patient?active=true");
		int batchSize = 2;
		input.addParameter(ProviderConstants.OPERATION_DELETE_BATCH_SIZE, new DecimalType(batchSize));

		// execute
		myCaptureQueriesListener.clear();
		Parameters response = myClient
			.operation()
			.onServer()
			.named(ProviderConstants.OPERATION_DELETE_EXPUNGE)
			.withParameters(input)
			.execute();

		ourLog.debug(myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(response));

		String jobId = BatchHelperR4.jobIdFromBatch2Parameters(response);
		myBatch2JobHelper.awaitJobCompletion(jobId);
		int deleteCount = myCaptureQueriesListener.getDeleteQueries().size();

		myCaptureQueriesListener.logDeleteQueries();
		assertEquals(30, deleteCount);
	}

	private void validateNoErrors(List<BaseTask> tasks) {
		int total = 0;
		for (BaseTask next : tasks) {
			if (next.getError() != null) {
				fail(next.getError().toString());
			}
			total += next.getTaskCount();
		}

		ourLog.info("Loaded {} searches", total);
	}

	private String jobExecutionIdFromOutcome(DeleteMethodOutcome theResult) {
		OperationOutcome operationOutcome = (OperationOutcome) theResult.getOperationOutcome();
		String diagnostics = operationOutcome.getIssueFirstRep().getDiagnostics();
		String[] parts = diagnostics.split("Delete job submitted with id ");
		return parts[1];
	}

	private final class SearchTask extends BaseTask {

		@Override
		public void run() {
			CloseableHttpResponse getResp;
			for (int i = 0; i < 10; i++) {
				try {
					Bundle respBundle;

					// Load search
					HttpGet get = new HttpGet(myServerBase + "/Patient?identifier=http%3A%2F%2Ftest%7CBAR," + UUID.randomUUID());
					get.addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON_NEW);
					getResp = ourHttpClient.execute(get);
					try {
						String respBundleString = IOUtils.toString(getResp.getEntity().getContent(), Charsets.UTF_8);
						assertThat(getResp.getStatusLine().getStatusCode()).as(respBundleString).isEqualTo(200);
						respBundle = myFhirContext.newJsonParser().parseResource(Bundle.class, respBundleString);
						myTaskCount++;
					} finally {
						IOUtils.closeQuietly(getResp);
					}

					// Load page 2
					get = new HttpGet(respBundle.getLink("next").getUrl());
					get.addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON_NEW);
					getResp = ourHttpClient.execute(get);
					try {
						assertEquals(200, getResp.getStatusLine().getStatusCode());
						myTaskCount++;
					} finally {
						IOUtils.closeQuietly(getResp);
					}

				} catch (Throwable e) {
					ourLog.error("Failure during search", e);
					myError = e;
					return;
				}
			}
		}
	}

	private final class CreateTask extends BaseTask {

		@Override
		public void run() {
			for (int i = 0; i < 50; i++) {
				try {
					Patient p = new Patient();
					p.addIdentifier().setSystem("http://test").setValue("BAR").setType(new CodeableConcept().addCoding(new Coding().setSystem("http://foo").setCode("bar")));
					p.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);
					myClient.create().resource(p).execute();

					mySearchParamRegistry.forceRefresh();

				} catch (Throwable e) {
					ourLog.error("Failure during search", e);
					myError = e;
					return;
				}
			}
		}
	}

	public static class BaseTask extends Thread {
		protected Throwable myError;
		protected int myTaskCount = 0;

		public BaseTask() {
			setDaemon(true);
		}

		public Throwable getError() {
			return myError;
		}

		public int getTaskCount() {
			return myTaskCount;
		}

	}


}
