package org.sagebionetworks.template.repo.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GridTemplatesTest {

	private Context requestContext;
	private Input requestInput;
	private Util util;

	private StringResourceRepository repo;
	private VelocityEngine engine;
	private VelocityContext context;

	@BeforeEach
	public void before() {

		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
		p.setProperty("string.resource.loader.class", StringResourceLoader.class.getName());
		p.setProperty("string.resource.loader.repository.static", "false");
		p.setProperty("runtime.references.strict", "true");

		engine = new VelocityEngine(p);
		engine.init();

		repo = (StringResourceRepository) engine.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);

		requestContext = new Context();
		requestInput = new Input();
		util = new Util();

		context = new VelocityContext();
		context.put("context", requestContext);
		context.put("input", requestInput);
		context.put("util", util);
	}

	@Test
	public void testConnect() throws IOException {
		requestContext.setConnectionId("con3333");
		requestContext.setEventType("CONNECT");
		requestInput.addParams("gridSessionId", "session5555").addParams("replicaId", "222").addParams("userId", "987");

		// call under test
		Template template = loadEscapedTemplate("templates/repo/grid/connect-request-template.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String rawResult = stringWriter.toString();
		String[] resultSplit = rawResult.split("&");
		assertEquals(11, resultSplit.length);
		assertEquals("Action=SendMessage", resultSplit[0]);
		String[] message = resultSplit[1].split("=");
		assertEquals(2, message.length);
		assertEquals("MessageBody", message[0]);
		// body must be URL encoded.
		JSONArray body = new JSONArray(java.net.URLDecoder.decode(message[1], StandardCharsets.UTF_8));
		assertEquals(
				"[8,\"connection\",{\"gridSessionId\":\"session5555\",\"replicaId\":222,\"userId\":987}]",
				body.toString());
		// ConnectionId
		assertEquals("MessageAttribute.1.Name=ConnectionId", resultSplit[2]);
		assertEquals("MessageAttribute.1.Value.DataType=String", resultSplit[3]);
		assertEquals("MessageAttribute.1.Value.StringValue=con3333", resultSplit[4]);
		// EventType
		assertEquals("MessageAttribute.2.Name=EventType", resultSplit[5]);
		assertEquals("MessageAttribute.2.Value.DataType=String", resultSplit[6]);
		assertEquals("MessageAttribute.2.Value.StringValue=CONNECT", resultSplit[7]);
		// EventSource
		assertEquals("MessageAttribute.3.Name=EventSource", resultSplit[8]);
		assertEquals("MessageAttribute.3.Value.DataType=String", resultSplit[9]);
		assertEquals("MessageAttribute.3.Value.StringValue=WEBSOCKET", resultSplit[10]);
	}

	@Test
	public void testDefault() throws IOException {
		requestContext.setConnectionId("con3333");
		requestContext.setEventType("MESSAGE");
		requestInput.setBody("[1,2]");

		// call under test
		Template template = loadEscapedTemplate("templates/repo/grid/default-request-template.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String rawResult = stringWriter.toString();
		String[] resultSplit = rawResult.split("&");
		assertEquals(11, resultSplit.length);
		assertEquals("Action=SendMessage", resultSplit[0]);
		String[] message = resultSplit[1].split("=");
		assertEquals(2, message.length);
		assertEquals("MessageBody", message[0]);
		// body must be URL encoded.
		JSONArray body = new JSONArray(java.net.URLDecoder.decode(message[1], StandardCharsets.UTF_8));
		assertEquals("[1,2]", body.toString());

		// ConnectionId
		assertEquals("MessageAttribute.1.Name=ConnectionId", resultSplit[2]);
		assertEquals("MessageAttribute.1.Value.DataType=String", resultSplit[3]);
		assertEquals("MessageAttribute.1.Value.StringValue=con3333", resultSplit[4]);
		// EventType
		assertEquals("MessageAttribute.2.Name=EventType", resultSplit[5]);
		assertEquals("MessageAttribute.2.Value.DataType=String", resultSplit[6]);
		assertEquals("MessageAttribute.2.Value.StringValue=MESSAGE", resultSplit[7]);
		// EventSource
		assertEquals("MessageAttribute.3.Name=EventSource", resultSplit[8]);
		assertEquals("MessageAttribute.3.Value.DataType=String", resultSplit[9]);
		assertEquals("MessageAttribute.3.Value.StringValue=WEBSOCKET", resultSplit[10]);
	}

	@Test
	public void testDisconnect() throws IOException {
		requestContext.setConnectionId("con3333");
		requestContext.setEventType("DISCONNECT");
		requestInput.setBody("[1,2]");

		// call under test
		Template template = loadEscapedTemplate("templates/repo/grid/disconnect-request-template.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String rawResult = stringWriter.toString();
		String[] resultSplit = rawResult.split("&");
		assertEquals(11, resultSplit.length);
		assertEquals("Action=SendMessage", resultSplit[0]);
		String[] message = resultSplit[1].split("=");
		assertEquals(2, message.length);
		assertEquals("MessageBody", message[0]);
		// body must be URL encoded.
		JSONArray body = new JSONArray(java.net.URLDecoder.decode(message[1], StandardCharsets.UTF_8));
		assertEquals("[8,\"disconnected\"]", body.toString());

		// ConnectionId
		assertEquals("MessageAttribute.1.Name=ConnectionId", resultSplit[2]);
		assertEquals("MessageAttribute.1.Value.DataType=String", resultSplit[3]);
		assertEquals("MessageAttribute.1.Value.StringValue=con3333", resultSplit[4]);
		// EventType
		assertEquals("MessageAttribute.2.Name=EventType", resultSplit[5]);
		assertEquals("MessageAttribute.2.Value.DataType=String", resultSplit[6]);
		assertEquals("MessageAttribute.2.Value.StringValue=DISCONNECT", resultSplit[7]);
		// EventSource
		assertEquals("MessageAttribute.3.Name=EventSource", resultSplit[8]);
		assertEquals("MessageAttribute.3.Value.DataType=String", resultSplit[9]);
		assertEquals("MessageAttribute.3.Value.StringValue=WEBSOCKET", resultSplit[10]);
	}

	@Test
	public void testAccessLog() throws IOException {
		requestContext.requestId = "re44";
		requestContext.setConnectionId("con3333").setRequestTime("the-time").setHttpMethod("PUT")
				.setResourcePath("foo/bar").setStatus("okay").setProtocol("http").setResponseLength(134L);
		requestContext.setIdentity(new Identity().setSourceIp("100.101.3.1").setCaller("aCaller").setUser("aUser"));
		requestInput.setBody("[1,2]");

		// call under test
		Template template = loadEscapedTemplate("templates/repo/grid/access-logs-setting-format.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String rawResult = stringWriter.toString();
		JSONObject jsonResult = new JSONObject(rawResult);
		assertEquals("{\"requestId\":\"re44\",\"ip\":\"100.101.3.1\",\"caller\":\"aCaller\","
				+ "\"user\":\"aUser\",\"requestTime\":\"the-time\",\"httpMethod\":\"PUT\","
				+ "\"resourcePath\":\"foo/bar\",\"status\":\"okay\",\"protocol\":\"http\",\"responseLength\":\"134\"}",
				jsonResult.toString());

	}

	/**
	 * Store the JSON escaped tempalte into the Velocity string resource where it
	 * can then be loaded as a Velocity template.
	 * 
	 * @param tempalte
	 * @return
	 */
	Template loadEscapedTemplate(String tempalte) {
		String tempString = escapeJsonTemplate(tempalte);
		String templateName = "exampleTemplate";
		repo.putStringResource(templateName, tempString);
		return engine.getTemplate(templateName);
	}

	/**
	 * Each template will be stored as a value in a JSON object. This method
	 * simulates that by loading the template into JSON then fetching the escaped
	 * template from the JSON object.
	 * 
	 * @param template
	 * @return
	 * @throws IOException
	 */
	public String escapeJsonTemplate(String template) {
		// The template will first be added a JSON template as a string.
		StringBuilder builder = new StringBuilder("{ \"someKey\":\"");
		builder.append(GridContextProvider.loadFileRemoveLineBreaks(template));
		builder.append("\" }");
		JSONObject object = new JSONObject(builder.toString());
		// This provides the JSON escaped value of the template.
		return object.getString("someKey");
	}

	/**
	 * @see <a href=
	 *      "https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-websocket-api-mapping-template-reference.html">apigateway-websocket-api-mapping-template-reference</a>
	 */
	public static class Context {

		private String connectionId;
		private String requestId;
		private Identity identity;
		private String requestTime;
		private String httpMethod;
		private String resourcePath;
		private String status;
		private String protocol;
		private Long responseLength;
		private String eventType;

		public String getConnectionId() {
			return connectionId;
		}

		public String getRequestId() {
			return requestId;
		}

		public Context setRequestId(String requestId) {
			this.requestId = requestId;
			return this;
		}

		public Context setConnectionId(String connectionId) {
			this.connectionId = connectionId;
			return this;
		}

		public Identity getIdentity() {
			return identity;
		}

		public Context setIdentity(Identity identity) {
			this.identity = identity;
			return this;
		}

		public String getRequestTime() {
			return requestTime;
		}

		public Context setRequestTime(String requestTime) {
			this.requestTime = requestTime;
			return this;
		}

		public String getHttpMethod() {
			return httpMethod;
		}

		public Context setHttpMethod(String httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		public String getResourcePath() {
			return resourcePath;
		}

		public Context setResourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
			return this;
		}

		public String getStatus() {
			return status;
		}

		public Context setStatus(String status) {
			this.status = status;
			return this;
		}

		public String getProtocol() {
			return protocol;
		}

		public Context setProtocol(String protocol) {
			this.protocol = protocol;
			return this;
		}

		public Long getResponseLength() {
			return responseLength;
		}

		public Context setResponseLength(Long responseLength) {
			this.responseLength = responseLength;
			return this;
		}

		public String getEventType() {
			return eventType;
		}

		public Context setEventType(String eventType) {
			this.eventType = eventType;
			return this;
		}

	}

	public static class Identity {
		private String sourceIp;
		private String caller;
		private String user;

		public String getSourceIp() {
			return sourceIp;
		}

		public Identity setSourceIp(String sourceIp) {
			this.sourceIp = sourceIp;
			return this;
		}

		public String getCaller() {
			return caller;
		}

		public Identity setCaller(String caller) {
			this.caller = caller;
			return this;
		}

		public String getUser() {
			return user;
		}

		public Identity setUser(String user) {
			this.user = user;
			return this;
		}

	}

	/**
	 * @see <a href=
	 *      "https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-mapping-template-reference.html#input-variable-reference">input-variable-reference</a>
	 */
	public static class Input {

		private Map<String, String> params = new HashMap<>();
		private String body;

		public Map<String, String> getParams() {
			return params;
		}

		public Input addParams(String key, String value) {
			this.params.put(key, value);
			return this;
		}

		public String getBody() {
			return body;
		}

		public Input setBody(String body) {
			this.body = body;
			return this;
		}

		public String params(String key) {
			return params.get(key);
		}

	}

	/**
	 * @see <a href=
	 *      "https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-websocket-api-mapping-template-reference.html">apigateway-websocket-api-mapping-template-reference</a>
	 */
	public static class Util {

		public String urlEncode(String toEncode) {
			return java.net.URLEncoder.encode(toEncode, StandardCharsets.UTF_8);
		}
	}

}
