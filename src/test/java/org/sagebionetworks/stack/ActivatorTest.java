package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeInfo;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ChangeStatus;
import com.amazonaws.services.route53.model.GetChangeRequest;
import com.amazonaws.services.route53.model.GetChangeResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

/**
 *
 * @author xavier
 */
public class ActivatorTest {
	
	Activator activator;
	InputConfiguration config;
	Properties props;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AmazonRoute53Client mockClient;
	AmazonS3Client mockS3Client;
	
	public ActivatorTest() {
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() throws IOException {
		config = TestHelper.createActivatorTestConfiguration();
		mockClient = factory.createRoute53Client();
		mockS3Client = factory.createS3Client();
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void testGetHostedZoneByName() throws IOException {
		activator = new Activator(factory, config, "123-0", "staging");
		List<HostedZone> expectedListHostedZones = new ArrayList<HostedZone>();
		HostedZone hostedZone = new HostedZone().withName("hostedZone1");
		expectedListHostedZones.add(hostedZone);
		hostedZone = new HostedZone().withName("hostedZone2");
		expectedListHostedZones.add(hostedZone);
		ListHostedZonesResult expectedLhzRes = new ListHostedZonesResult();
		expectedLhzRes.setHostedZones(expectedListHostedZones);
		when(mockClient.listHostedZones()).thenReturn(expectedLhzRes);
		assertNull(activator.getHostedZoneByName("hostedZone0"));
		HostedZone actualHostedZone = activator.getHostedZoneByName("hostedZone1");
		assertNotNull(actualHostedZone);
		assertEquals("hostedZone1", actualHostedZone.getName());
	}
	
	@Test
	public void testGetResourceRecordSetByCNAME() throws IOException {
		// Setup expected ListResourceRecordSet requests and results
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
		req.setHostedZoneId("zoneId");
		req.setStartRecordType(RRType.CNAME);
		req.setStartRecordName("cname1");
		req.setMaxItems("1");
		//	Error case, the cname is not in the zone
		Collection<ResourceRecordSet> rrsList = new ArrayList<ResourceRecordSet>();
		ListResourceRecordSetsResult res = new ListResourceRecordSetsResult().withResourceRecordSets(rrsList);
		when(mockClient.listResourceRecordSets(req)).thenReturn(res);
		activator = new Activator(factory, config, "123-0", "staging");
		ResourceRecordSet rrs = null;
		try {
			rrs = activator.getResourceRecordSetByCNAME("zoneId", "cname1");
		} catch (IllegalArgumentException e) {
			assertNull(rrs);
		}
		//	OK case, cname found
		ResourceRecordSet rrs1 = new ResourceRecordSet().withName("cname1").withType(RRType.CNAME);
		rrsList.add(rrs1);
		res.setResourceRecordSets(rrsList);
		when(mockClient.listResourceRecordSets(req)).thenReturn(res);
		rrs = activator.getResourceRecordSetByCNAME("zoneId", "cname1");
		assertNotNull(rrs);
		assertEquals("cname1", rrs.getName());
		assertEquals(RRType.CNAME.toString(), rrs.getType());
	}
	
	@Test
	public void testCreateChangeList() throws IOException {
		ResourceRecordSet rrsOld = new ResourceRecordSet().withName("cname").withType(RRType.CNAME);
		String targetName = "adomain.org";
		List<Change> expectedChanges = new ArrayList<Change>();
		Change chg = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(rrsOld);
		expectedChanges.add(chg);
		ResourceRecordSet rrsNew = new ResourceRecordSet().withName("cname").withType(RRType.CNAME).withResourceRecords(new ResourceRecord().withValue(targetName)).withTTL(300L);
		chg = new Change().withResourceRecordSet(rrsNew).withAction(ChangeAction.CREATE);
		expectedChanges.add(chg);
		activator = new Activator(factory, config, "123-0", "staging");
		List<Change> changes = activator.createChangeList(rrsOld, targetName);
		assertNotNull(changes);
		assertEquals(expectedChanges, changes);
	}
	
	@Test
	public void testApplyChanges() throws IOException {
		String hostedZoneId = "zoneId";
		List<Change> changes = new ArrayList<Change>();
		Change chg = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(new ResourceRecordSet().withName("rrs1").withType(RRType.CNAME));
		changes.add(chg);
		ResourceRecord rr = new ResourceRecord().withValue("newName");
		chg = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(new ResourceRecordSet().withName("rrs1").withType(RRType.CNAME).withResourceRecords(rr));
		changes.add(chg);
		// Request
		String swapComment = "StackActivator - making stack: 123-0 to staging.";
		ChangeBatch batch = new ChangeBatch().withChanges(changes).withComment(swapComment);
		ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest().withChangeBatch(batch).withHostedZoneId(hostedZoneId);
		GetChangeRequest creq = new GetChangeRequest().withId("id");
		// Expected results
		ChangeInfo ciInProgress = new ChangeInfo().withStatus(ChangeStatus.InProgress).withId("id");
		ChangeInfo ciDeployed = new ChangeInfo().withStatus(ChangeStatus.Deployed).withId("id");
		ChangeResourceRecordSetsResult crrsrExpected = new ChangeResourceRecordSetsResult().withChangeInfo(ciInProgress);
		GetChangeResult gcrExpectedInProgress = new GetChangeResult().withChangeInfo(ciInProgress);
		GetChangeResult gcrExpectedDeployed = new GetChangeResult().withChangeInfo(ciDeployed);
		when(mockClient.changeResourceRecordSets(req)).thenReturn(crrsrExpected);
		when(mockClient.getChange(creq)).thenReturn(gcrExpectedInProgress, gcrExpectedInProgress, gcrExpectedDeployed);
		activator = new Activator(factory, config, "123-0", "staging");
		activator.applyChanges(hostedZoneId, changes);
		verify(mockClient, times(3)).getChange(creq);
	}
	
	@Test
	public void testCreateJSONStackActivationRecord() throws IOException {
		activator = new Activator(factory, config, "1234-0", "prod");
		long time = System.currentTimeMillis();
		String activationTime = Long.toString(time);
		String instance = "1234-0";
		JSONObject expectedJSON = new JSONObject();
		expectedJSON.put("activationtime", activationTime);
		expectedJSON.put("instance", instance);
		JSONObject actualJSON = activator.createJSONActivationRecord(time, instance);
		assertEquals(expectedJSON, actualJSON);
	}
	
	@Test
	public void testSaveJSONStackActivationRecord() throws IOException, ParseException {
		JSONObject inputJSON = new JSONObject();
		inputJSON.put("somekey", "somevalue");
		activator = new Activator(factory, config, "1234-0", "prod");
		File tmp = activator.saveJSONActivationRecord("1234-0", inputJSON);
		assertNotNull(tmp);
		assertTrue(tmp.exists());
		assertTrue(tmp.isFile());
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(new FileReader(tmp.getAbsolutePath()));
		JSONObject outputJSON = (JSONObject) obj;
		assertEquals(inputJSON, outputJSON);
	}
	
	@Test
	public void uploadJSONActivationRecordFile() throws IOException {
		activator = new Activator(factory, config, "1234-0", "prod");
		File tmp = File.createTempFile("file", ".json");
		PutObjectResult expectedRes = new PutObjectResult();
		//when(mockS3Client.putObject(config.getStackActivationLogS3BucketName(), config.getStackActivationLogFileName(), tmp)).thenReturn(expectedRes);
		activator.uploadJSONActivationRecordFile(tmp);
		verify(mockS3Client).putObject(config.getStackActivationLogS3BucketName(), config.getStackActivationLogFileName(), tmp);
		tmp.delete();
	}
}
