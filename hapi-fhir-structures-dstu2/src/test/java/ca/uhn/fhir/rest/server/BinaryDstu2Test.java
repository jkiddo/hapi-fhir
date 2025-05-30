package ca.uhn.fhir.rest.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Binary;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.test.utilities.HttpClientExtension;
import ca.uhn.fhir.test.utilities.server.RestfulServerExtension;
import ca.uhn.fhir.util.TestUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by dsotnikov on 2/25/2014.
 */
public class BinaryDstu2Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BinaryDstu2Test.class);
	private static final FhirContext ourCtx = FhirContext.forDstu2Cached();
	private static Binary ourLast;

	@RegisterExtension
	public static final RestfulServerExtension ourServer = new RestfulServerExtension(ourCtx)
		.setDefaultResponseEncoding(EncodingEnum.XML)
		.registerProvider(new ResourceProvider())
		.withPagingProvider(new FifoMemoryPagingProvider(100))
		.setDefaultPrettyPrint(false);

	@RegisterExtension
	public static final HttpClientExtension ourClient = new HttpClientExtension();

	@BeforeEach
	public void before() {
		ourLast = null;
	}

	@Test
	public void testReadWithExplicitTypeXml() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/Binary/foo?_format=xml");
		try (CloseableHttpResponse response = ourClient.execute(httpGet)) {
			String responseContent = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			IOUtils.closeQuietly(response.getEntity().getContent());

			ourLog.info(responseContent);

			assertEquals(200, response.getStatusLine().getStatusCode());
			assertThat(response.getFirstHeader("content-type").getValue()).startsWith(Constants.CT_FHIR_XML + ";");

			Binary bin = ourCtx.newXmlParser().parseResource(Binary.class, responseContent);
			assertEquals("foo", bin.getContentType());
			assertThat(bin.getContent()).containsExactly(new byte[]{1, 2, 3, 4});
		}
	}

	@Test
	public void testReadWithExplicitTypeJson() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/Binary/foo?_format=json");
		try (CloseableHttpResponse response = ourClient.execute(httpGet)) {
			String responseContent = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			IOUtils.closeQuietly(response.getEntity().getContent());

			ourLog.info(responseContent);

			assertEquals(200, response.getStatusLine().getStatusCode());
			assertThat(response.getFirstHeader("content-type").getValue()).startsWith(Constants.CT_FHIR_JSON + ";");

			Binary bin = ourCtx.newJsonParser().parseResource(Binary.class, responseContent);
			assertEquals("foo", bin.getContentType());
			assertThat(bin.getContent()).containsExactly(new byte[]{1, 2, 3, 4});
		}
	}

	// posts Binary directly
	@Test
	public void testPostBinary() throws Exception {
		HttpPost http = new HttpPost(ourServer.getBaseUrl() + "/Binary");
		http.setEntity(new ByteArrayEntity(new byte[]{1, 2, 3, 4}, ContentType.create("foo/bar", "UTF-8")));

		try (CloseableHttpResponse response = ourClient.execute(http)) {
			assertEquals(201, response.getStatusLine().getStatusCode());

			assertEquals("foo/bar; charset=UTF-8", ourLast.getContentType());
			assertThat(ourLast.getContent()).containsExactly(new byte[]{1, 2, 3, 4});
		}
	}

	// posts Binary as FHIR Resource
	@Test
	public void testPostFhirBinary() throws Exception {
		Binary res = new Binary();
		res.setContent(new byte[]{1, 2, 3, 4});
		res.setContentType("text/plain");
		String stringContent = ourCtx.newJsonParser().encodeResourceToString(res);

		HttpPost http = new HttpPost(ourServer.getBaseUrl() + "/Binary");
		http.setEntity(new StringEntity(stringContent, ContentType.create(Constants.CT_FHIR_JSON, "UTF-8")));

		try (CloseableHttpResponse response = ourClient.execute(http)) {
			assertEquals(201, response.getStatusLine().getStatusCode());

			assertEquals("text/plain", ourLast.getContentType().replace(" ", "").toLowerCase());
		}
	}

	@Test
	public void testBinaryReadAcceptMissing() throws Exception {
		HttpGet http = new HttpGet(ourServer.getBaseUrl() + "/Binary/foo");

		binaryRead(http);
	}

	@Test
	public void testBinaryReadAcceptBrowser() throws Exception {
		HttpGet http = new HttpGet(ourServer.getBaseUrl() + "/Binary/foo");
		http.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1");
		http.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		binaryRead(http);
	}

	private void binaryRead(HttpGet http) throws IOException {
		try (CloseableHttpResponse status = ourClient.execute(http)) {
			byte[] responseContent = IOUtils.toByteArray(status.getEntity().getContent());
			IOUtils.closeQuietly(status.getEntity().getContent());
			assertEquals(200, status.getStatusLine().getStatusCode());
			assertEquals("foo", status.getFirstHeader("content-type").getValue());
			assertEquals("Attachment;", status.getFirstHeader("Content-Disposition").getValue()); // This is a security requirement!
			assertThat(responseContent).containsExactly(new byte[]{1, 2, 3, 4});
		}
	}

	@Test
	public void testBinaryReadAcceptFhirJson() throws Exception {
		HttpGet http = new HttpGet(ourServer.getBaseUrl() + "/Binary/foo");
		http.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1");
		http.addHeader("Accept", Constants.CT_FHIR_JSON);

		try (CloseableHttpResponse status = ourClient.execute(http)) {
			String responseContent = IOUtils.toString(status.getEntity().getContent(), StandardCharsets.UTF_8);
			IOUtils.closeQuietly(status.getEntity().getContent());
			assertEquals(200, status.getStatusLine().getStatusCode());
			assertEquals(Constants.CT_FHIR_JSON + ";charset=utf-8", status.getFirstHeader("content-type").getValue().replace(" ", "").toLowerCase());
			assertNull(status.getFirstHeader("Content-Disposition"));
			assertEquals("{\"resourceType\":\"Binary\",\"id\":\"1\",\"contentType\":\"foo\",\"content\":\"AQIDBA==\"}", responseContent);
		}
	}

	@Test
	public void testSearchJson() throws Exception {
		HttpGet http = new HttpGet(ourServer.getBaseUrl() + "/Binary?_pretty=true&_format=json");
		try (CloseableHttpResponse response = ourClient.execute(http)) {
			String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			IOUtils.closeQuietly(response.getEntity().getContent());
			assertEquals(200, response.getStatusLine().getStatusCode());
			assertEquals(Constants.CT_FHIR_JSON + ";charset=utf-8", response.getFirstHeader("content-type").getValue().replace(" ", "").replace("UTF", "utf"));

			ourLog.info(responseContent);

			Bundle bundle = ourCtx.newJsonParser().parseResource(Bundle.class, responseContent);
			Binary bin = (Binary) bundle.getEntry().get(0).getResource();

			assertEquals("text/plain", bin.getContentType());
			assertThat(bin.getContent()).containsExactly(new byte[]{1, 2, 3, 4});
		}
	}

	@Test
	public void testSearchXml() throws Exception {
		HttpGet http = new HttpGet(ourServer.getBaseUrl() + "/Binary?_pretty=true");
		try (CloseableHttpResponse response = ourClient.execute(http)) {
			String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			IOUtils.closeQuietly(response.getEntity().getContent());
			assertEquals(200, response.getStatusLine().getStatusCode());
			assertEquals(Constants.CT_FHIR_XML + ";charset=utf-8", response.getFirstHeader("content-type").getValue().replace(" ", "").replace("UTF", "utf"));

			ourLog.info(responseContent);

			Bundle bundle = ourCtx.newXmlParser().parseResource(Bundle.class, responseContent);
			Binary bin = (Binary) bundle.getEntry().get(0).getResource();

			assertEquals("text/plain", bin.getContentType());
			assertThat(bin.getContent()).containsExactly(new byte[]{1, 2, 3, 4});
		}
	}

	public static class ResourceProvider implements IResourceProvider {

		@Create
		public MethodOutcome create(@ResourceParam Binary theBinary) {
			ourLast = theBinary;
			return new MethodOutcome(new IdDt("1"));
		}

		@Override
		public Class<? extends IResource> getResourceType() {
			return Binary.class;
		}

		@Read
		public Binary read(@IdParam IdDt theId) {
			Binary retVal = new Binary();
			retVal.setId("1");
			retVal.setContent(new byte[]{1, 2, 3, 4});
			retVal.setContentType(theId.getIdPart());
			return retVal;
		}

		@Search
		public List<Binary> search() {
			Binary retVal = new Binary();
			retVal.setId("1");
			retVal.setContent(new byte[]{1, 2, 3, 4});
			retVal.setContentType("text/plain");
			return Collections.singletonList(retVal);
		}
	}

	@AfterAll
	public static void afterClassClearContext() throws Exception {
		TestUtil.randomizeLocaleAndTimezone();
	}

}
