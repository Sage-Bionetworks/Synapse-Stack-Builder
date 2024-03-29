package org.sagebionetworks.template.repo.athena;

import static org.sagebionetworks.template.Constants.ATHENA_QUERY_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.ATHENA_QUERY_DATA_BUCKETS;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sagebionetworks.template.repo.VelocityContextProvider;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseVelocityContextProvider;

import com.google.inject.Inject;

public class RecurrentAthenaQueryContextProvider implements VelocityContextProvider {

	public static final String DEFAULT_DATABASE = KinesisFirehoseVelocityContextProvider.GLUE_DB_SUFFIX;
	
	private RecurrentAthenaQueryConfig config;

	@Inject
	public RecurrentAthenaQueryContextProvider(RecurrentAthenaQueryConfig config) {
		this.config = config;
	}

	@Override
	public void addToContext(VelocityContext context) {

		List<RecurrentAthenaQuery> queries = config.getQueries();

		if (queries == null) {
			queries = Collections.emptyList();
		}

		Set<String> buckets = new HashSet<>();
		queries.forEach((query) -> {
			query.setQueryString(processQueryString(context, query));
			if (query.getDatabase() == null) {
				query.setDatabase(DEFAULT_DATABASE);
			}
			buckets.add(processTemplate(context, query.getDataBucket()));
		});

		context.put(ATHENA_QUERY_DESCRIPTORS, queries);
		context.put(ATHENA_QUERY_DATA_BUCKETS, buckets);
	}
	
	private String processQueryString(VelocityContext context, RecurrentAthenaQuery query) {
		String queryStringTemplate = AthenaQueryUtils.loadQueryFromPath(query.getQueryPath());

		return processTemplate(context, queryStringTemplate);
	}
	
	private String processTemplate(VelocityContext context, String stringTemplate) {
		StringWriter writer = new StringWriter();

		Velocity.evaluate(context, writer, "VTL", stringTemplate);
		
		return StringUtils.normalizeSpace(writer.toString());
	}

}
