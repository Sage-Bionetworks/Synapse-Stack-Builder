package org.sagebionetworks.template.repo.agent;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.StringJoiner;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class BedrockAgentContextProvider implements VelocityContextProvider {

	private final RepoConfiguration repoConfig;
	private final S3Client s3Client;

	@Inject
	public BedrockAgentContextProvider(RepoConfiguration repoConfig, S3Client s3Client) {
		super();
		this.repoConfig = repoConfig;
		this.s3Client = s3Client;
	}

	@Override
	public void addToContext(VelocityContext context) {
		String stack = repoConfig.getProperty(PROPERTY_KEY_STACK);
		String instance = repoConfig.getProperty(PROPERTY_KEY_INSTANCE);
		String agentName = new StringJoiner("-").add(stack).add(instance).add("agent").toString();
		String globalStackPrefix = Constants.createGlobalResourcesExportPrefix(stack);
		
		String openApiSchemaBucket = String.format("%s-configuration.sagebase.org", stack);
		String openApiSchemakey = String.format("chat/openapi/%s.json", instance);

		String openApiSchemJsonString = TemplateUtils.loadContentFromFile("templates/repo/agent/agent_open_api.json");
		s3Client.putObject(
			PutObjectRequest.builder()
				.bucket(openApiSchemaBucket)
				.key(openApiSchemakey)
				.build(),
			RequestBody.fromString(openApiSchemJsonString)
		);
		
		String openApiSchemaS3Arn = String.format("arn:aws:s3:::%s/%s", openApiSchemaBucket, openApiSchemakey);

		JSONObject baseTemplate = new JSONObject(TemplateUtils.loadContentFromFile("templates/repo/agent/bedrock_agent_template.json"));

		JSONObject resources = baseTemplate.getJSONObject("Resources");
		
		JSONArray roleStatements = resources
				.getJSONObject("bedrockAgentRole")
				.getJSONObject("Properties")
				.getJSONArray("Policies")
				.getJSONObject(0)
				.getJSONObject("PolicyDocument")
				.getJSONArray("Statement");
		
		// Since the agent template is shared to external people, we need to hack it to replace parameters that do not exist in our template
		JSONArray bedrockAgentRoleKbResource = roleStatements
			.getJSONObject(1)
			.getJSONArray("Fn::If")
			.getJSONObject(1)
			.getJSONArray("Resource");
		
		bedrockAgentRoleKbResource.put(0, new JSONObject().put("Fn::ImportValue", globalStackPrefix + "-SynapseHelpKnowledgeBaseArn"));
		
		JSONObject bedrockAgentProps = resources.getJSONObject("bedrockAgent").getJSONObject("Properties");
		
		JSONObject kbProperty = bedrockAgentProps
			.getJSONObject("KnowledgeBases")
			.getJSONArray("Fn::If")
			.getJSONArray(1)
			.getJSONObject(0);
		
		kbProperty.put("KnowledgeBaseId", new JSONObject().put("Fn::ImportValue", globalStackPrefix  + "-SynapseHelpKnowledgeBaseId"));
		kbProperty.put("Description", baseTemplate.getJSONObject("Parameters").getJSONObject("knowledgeBaseDescription").getString("Default"));
		
		
		// set bucket and key
		JSONObject statementTwo = roleStatements.getJSONObject(2);
		statementTwo.put("Resource", openApiSchemaS3Arn);
		
		JSONObject s3 = bedrockAgentProps.getJSONArray("ActionGroups").getJSONObject(1).getJSONObject("ApiSchema")
				.getJSONObject("S3");
		s3.put("S3BucketName", openApiSchemaBucket);
		s3.put("S3ObjectKey", openApiSchemakey);
		
		bedrockAgentProps.put("AgentName", agentName);
		String json = resources.toString();
		context.put("bedrock_agent_resouces", "," + json.substring(1, json.length()-1));

	}

}
