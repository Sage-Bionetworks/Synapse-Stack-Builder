package org.sagebionetworks.template;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.Test;

public class VpcTempalteTest {

	@Test
	public void testMergeTempalte() {
		VelocityContext context = new VelocityContext();
		List<String> colors = new LinkedList<>();
		colors.add("Red");
		colors.add("Blue");
		context.put("colors", colors);
		
		VelocityEngine ve = VelocityUtils.createEngine();
		
		Template template = ve.getTemplate("templates/vpc/main-vpc.json.vtp");;
		StringWriter sw = new StringWriter();

		template.merge(context, sw);
		// should parse to JSON.
		JSONObject json = new JSONObject(sw.toString());
		assertNotNull(json);
		System.out.println(json.toString(3));
		
		JSONObject resouces = json.getJSONObject("Resources");
		assertNotNull(resouces);
		assertTrue(resouces.has("VPC"));
		assertTrue(resouces.has("InternetGateway"));
		assertTrue(resouces.has("GatewayToInternet"));
		assertTrue(resouces.has("PublicRouteTable"));
		assertTrue(resouces.has("PublicNetworkAcl"));
		assertTrue(resouces.has("PublicRoute"));
		assertTrue(resouces.has("InboundHTTPPublicNetworkAclEntry"));
		assertTrue(resouces.has("OutboundPublicNetworkAclEntry"));
		assertTrue(resouces.has("ElasticIP"));
		assertTrue(resouces.has("PrivateRouteTable"));
		assertTrue(resouces.has("VpnSecurityGroup"));
		// color subnets
		// red
		assertTrue(resouces.has("RedPublic1Subnet"));
		assertTrue(resouces.has("RedPublic2Subnet"));
		assertTrue(resouces.has("RedPrivate1Subnet"));
		assertTrue(resouces.has("RedPrivate2Subnet"));
		assertTrue(resouces.has("RedPublic1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPublic2SubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPublic1SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("RedPublic2SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("RedPrivate1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPrivate2SubnetRouteTableAssociation"));
		// blue
		assertTrue(resouces.has("BluePublic1Subnet"));
		assertTrue(resouces.has("BluePublic2Subnet"));
		assertTrue(resouces.has("BluePrivate1Subnet"));
		assertTrue(resouces.has("BluePrivate2Subnet"));
		assertTrue(resouces.has("BluePublic1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("BluePublic2SubnetRouteTableAssociation"));
		assertTrue(resouces.has("BluePublic1SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("BluePublic2SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("BluePrivate1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("BluePrivate2SubnetRouteTableAssociation"));
		
	}
}
