package org.sagebionetworks.template.repo.grid;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GridContextProvider implements VelocityContextProvider {

	private final String gridQueueReferenceName;

	@Inject
	public GridContextProvider(@Named("GridQueueReferenceName") String gridQueueReferenceName) {
		super();
		this.gridQueueReferenceName = gridQueueReferenceName;

	}

	@Override
	public void addToContext(VelocityContext context) {
		context.put("D", "$");
		context.put("gridWebsocketQueueRef", gridQueueReferenceName);
		context.put("connectRequestTempalte",	loadFileRemoveLineBreaks("templates/repo/grid/connect-request-template.vpt"));
		context.put("defaultRequestTempalte",	loadFileRemoveLineBreaks("templates/repo/grid/default-request-template.vpt"));
		context.put("disconnectRequestTempalte",loadFileRemoveLineBreaks("templates/repo/grid/disconnect-request-template.vpt"));
		context.put("accessLogsSettingFormat",	loadFileRemoveLineBreaks("templates/repo/grid/access-logs-setting-format.vpt"));
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
