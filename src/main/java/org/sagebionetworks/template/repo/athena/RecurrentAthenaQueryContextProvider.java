package org.sagebionetworks.template.repo.athena;

import static org.sagebionetworks.template.Constants.ATHENA_QUERY_DESCRIPTORS;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

public class RecurrentAthenaQueryContextProvider implements VelocityContextProvider {

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

		queries.forEach((query) -> {
			String queryStringTemplate = AthenaQueryUtils.loadQueryFromPath(query.getQueryPath());

			StringWriter writer = new StringWriter();

			Velocity.evaluate(context, writer, "VTL", queryStringTemplate);

			query.setQueryString(writer.toString());
		});

		context.put(ATHENA_QUERY_DESCRIPTORS, queries);
	}

}
