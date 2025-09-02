package org.sagebionetworks.template.repo.agent;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.StringJoiner;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.amazonaws.services.s3.AmazonS3Client;

public class BedrockGridAgentContextProvider implements VelocityContextProvider {

	private final RepoConfiguration repoConfig;
	private final AmazonS3Client s3Cient;

	public BedrockGridAgentContextProvider(RepoConfiguration repoConfig, AmazonS3Client s3Cient) {
		super();
		this.repoConfig = repoConfig;
		this.s3Cient = s3Cient;
	}

	@Override
	public void addToContext(VelocityContext context) {
		String stack = repoConfig.getProperty(PROPERTY_KEY_STACK);
		String instance = repoConfig.getProperty(PROPERTY_KEY_INSTANCE);
		String agentName = new StringJoiner("-").add(stack).add(instance).add("grid").add("agent").toString();

		String openApiSchemaBucket = String.format("%s-configuration.sagebase.org", stack);
		String openApiSchemakey = String.format("chat/openapi/grid/%s.json", instance);

		String openApiSchemJsonString = TemplateUtils.loadContentFromFile("templates/repo/agent/grid_agent_open_api.json");
		s3Cient.putObject(openApiSchemaBucket, openApiSchemakey, openApiSchemJsonString);
		
		String openApiSchemaS3Arn = String.format("arn:aws:s3:::%s/%s", openApiSchemaBucket, openApiSchemakey);
		
		JSONObject baseTemplate = new JSONObject(TemplateUtils.loadContentFromFile("templates/repo/agent/grid_agent_template.json"));

		JSONObject resources = baseTemplate.getJSONObject("Resources");
		
		JSONArray roleStatements = resources
				.getJSONObject("bedrockGridAgentRole")
				.getJSONObject("Properties")
				.getJSONArray("Policies")
				.getJSONObject(0)
				.getJSONObject("PolicyDocument")
				.getJSONArray("Statement");
		
		// set bucket and key
		JSONObject statementTwo = roleStatements.getJSONObject(1);
		statementTwo.put("Resource", openApiSchemaS3Arn);
		
		JSONObject bedrockAgentProps = resources.getJSONObject("bedrockGridAgent").getJSONObject("Properties");
		
		JSONObject s3 = bedrockAgentProps.getJSONArray("ActionGroups").getJSONObject(0).getJSONObject("ApiSchema")
				.getJSONObject("S3");
		s3.put("S3BucketName", openApiSchemaBucket);
		s3.put("S3ObjectKey", openApiSchemakey);
		
		bedrockAgentProps.put("AgentName", agentName);
		String instructions = TemplateUtils.loadContentFromFile("templates/repo/agent/grid-agent-instructions.txt");
		bedrockAgentProps.put("Instructions", instructions);

		String json = resources.toString();
		context.put("bedrock_grid_agent_resouces", "," + json.substring(1, json.length()-1));
	}

}
