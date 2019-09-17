package org.sagebionetworks.template.repo.kinesis.firehose;

import static org.sagebionetworks.template.Constants.GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.KINESIS_FIREHOSE_STREAM_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

public class KinesisFirehoseVelocityContextProvider implements VelocityContextProvider {

	private static final String DEV_STACK_NAME = "dev";
	public static final String GLUE_DB_SUFFIX = "firehoseLogs";

	private KinesisFirehoseConfig config;
	private RepoConfiguration repoConfig;

	@Inject
	public KinesisFirehoseVelocityContextProvider(KinesisFirehoseConfig config, RepoConfiguration repoConfig) {
		this.config = config;
		this.repoConfig = repoConfig;
	}

	@Override
	public void addToContext(VelocityContext context) {
		config.getStreamDescriptors().forEach(this::postProcessStream);

		Set<KinesisFirehoseStreamDescriptor> streams = config.getStreamDescriptors();

		// Does not deploy to prod stacks that are dev only
		if (!getStack().equalsIgnoreCase(DEV_STACK_NAME)) {
			streams = streams.stream().filter(stream -> !stream.isDevOnly()).collect(Collectors.toSet());
		}

		context.put(GLUE_DATABASE_NAME, parameterizeWithInstance(GLUE_DB_SUFFIX));
		context.put(KINESIS_FIREHOSE_STREAM_DESCRIPTORS, streams);
	}

	private void postProcessStream(KinesisFirehoseStreamDescriptor stream) {
		if (stream == null) {
			return;
		}
		postProcessTable(stream.getTableDescriptor());
	}

	private void postProcessTable(GlueTableDescriptor table) {
		if (table == null) {
			return;
		}
		table.setName(parameterizeWithInstance(table.getName()));
	}

	private String parameterizeWithInstance(String value) {
		return getStack() + getInstance() + value;
	}

	private String getStack() {
		return repoConfig.getProperty(PROPERTY_KEY_STACK);
	}

	private String getInstance() {
		return repoConfig.getProperty(PROPERTY_KEY_INSTANCE);
	}

}
