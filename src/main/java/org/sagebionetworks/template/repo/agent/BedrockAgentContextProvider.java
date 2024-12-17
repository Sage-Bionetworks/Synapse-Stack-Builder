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

import com.google.inject.Inject;

public class BedrockAgentContextProvider implements VelocityContextProvider {
	
	private static final String SYNHELP_KNOWLEDGE_BASE_DESCRIPTION = "You can use this knowlegde base to answer questions on how to use synapse.";

	private final RepoConfiguration repoConfig;

	@Inject
	public BedrockAgentContextProvider(RepoConfiguration repoConfig) {
		super();
		this.repoConfig = repoConfig;
	}

	@Override
	public void addToContext(VelocityContext context) {
		String stack = repoConfig.getProperty(PROPERTY_KEY_STACK);
		String instance = repoConfig.getProperty(PROPERTY_KEY_INSTANCE);
		String agentName = new StringJoiner("-").add(stack).add(instance).add("agent").toString();

		JSONObject baseTemplate = new JSONObject(TemplateUtils.loadContentFromFile("templates/repo/agent/bedrock_agent_template.json"));

		JSONObject resources = baseTemplate.getJSONObject("Resources");
		
		// Since the agent template is shared to external people, we need to hack it to replace parameters that do not exist in our template
		JSONArray bedrockAgentRoleKbResource = resources
			.getJSONObject("bedrockAgentRole")
			.getJSONObject("Properties")
			.getJSONArray("Policies")
			.getJSONObject(0)
			.getJSONObject("PolicyDocument")
			.getJSONArray("Statement")
			.getJSONObject(1)
			.getJSONArray("Fn::If")
			.getJSONObject(1)
			.getJSONArray("Resource");
		
		bedrockAgentRoleKbResource.put(0, new JSONObject("{ \"Fn::GetAtt\": [\"SynapseHelpKnowledgeBase\", \"KnowledgeBaseArn\"] }"));
		
		JSONObject bedrockAgentProps = resources.getJSONObject("bedrockAgent").getJSONObject("Properties");
		
		JSONObject kbProperty = bedrockAgentProps
			.getJSONObject("KnowledgeBases")
			.getJSONArray("Fn::If")
			.getJSONArray(1)
			.getJSONObject(0);
			
		kbProperty.getJSONObject("KnowledgeBaseId").put("Ref", "SynapseHelpKnowledgeBase");
		kbProperty.put("Description", SYNHELP_KNOWLEDGE_BASE_DESCRIPTION);
		
		bedrockAgentProps.put("AgentName", agentName);
		String json = resources.toString();
		context.put("bedrock_agent_resouces", "," + json.substring(1, json.length()-1));

	}

}
