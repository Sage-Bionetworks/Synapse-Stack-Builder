package org.sagebionetworks.template.repo.grid;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GridContextProvider implements VelocityContextProvider {

	private final String gridQueueReferenceName;
	private final RepoConfiguration config;

	@Inject
	public GridContextProvider(@Named("GridQueueReferenceName") String gridQueueReferenceName, RepoConfiguration config) {
		super();
		this.gridQueueReferenceName = gridQueueReferenceName;
		this.config = config;

	}

	@Override
	public void addToContext(VelocityContext context) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		
		context.put("D", "$");
		context.put("gridWebsocketQueueRef", gridQueueReferenceName);
		context.put("connectRequestTempalte",	loadFileRemoveLineBreaks("templates/repo/grid/connect-request-template.vpt"));
		context.put("defaultRequestTempalte",	loadFileRemoveLineBreaks("templates/repo/grid/default-request-template.vpt"));
		context.put("disconnectRequestTempalte",loadFileRemoveLineBreaks("templates/repo/grid/disconnect-request-template.vpt"));
		context.put("accessLogsSettingFormat",	loadFileRemoveLineBreaks("templates/repo/grid/access-logs-setting-format.vpt"));
		String gridApiName = String.format("%s-%s-grid-websocket", stack, instance);
		context.put("gridWebsocketApiName", gridApiName);
	}
	
	/**
	 * Load a file from the classpath and remove line breaks.
	 * @param path
	 * @return
	 */
	public static String loadFileRemoveLineBreaks(String path) {
		return TemplateUtils.loadContentFromFile(path).replaceAll("\\R+", "");
	}

}
