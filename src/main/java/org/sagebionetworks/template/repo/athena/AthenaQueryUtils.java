package org.sagebionetworks.template.repo.athena;

import org.sagebionetworks.template.TemplateUtils;

public class AthenaQueryUtils {
	
	private static final String ATHENA_PREFIX = "athena/";

	public static String loadQueryFromPath(String queryPath) {
		return TemplateUtils.loadContentFromFile(ATHENA_PREFIX + queryPath);
	}

}
