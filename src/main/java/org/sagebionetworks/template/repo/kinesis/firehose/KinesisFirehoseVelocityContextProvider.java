package org.sagebionetworks.template.repo.kinesis.firehose;

import static org.sagebionetworks.template.Constants.GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.KINESIS_FIREHOSE_STREAM_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

public class KinesisFirehoseVelocityContextProvider implements VelocityContextProvider {

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
		context.put(GLUE_DATABASE_NAME, parameterizeWithInstance(GLUE_DB_SUFFIX));
		context.put(KINESIS_FIREHOSE_STREAM_DESCRIPTORS, config.getStreamDescriptors());

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
