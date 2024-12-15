package org.sagebionetworks.template.repo.agent;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.StringJoiner;

import org.apache.velocity.VelocityContext;
import org.json.JSONObject;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

public class BedrockAgentContextProvider implements VelocityContextProvider {

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
		JSONObject baseTemplate = new JSONObject(
				TemplateUtils.loadContentFromFile("templates/repo/agent/bedrock_agent_template.json"));

		JSONObject resources = baseTemplate.getJSONObject("Resources");
		JSONObject bedrockAgentProps = resources.getJSONObject("bedrockAgent").getJSONObject("Properties");
		bedrockAgentProps.put("AgentName", agentName);
		String json = resources.toString();
		context.put("bedrock_agent_resouces", "," + json.substring(1, json.length()-1));

	}

}
