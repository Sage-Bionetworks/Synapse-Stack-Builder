package org.sagebionetworks.template.repo.kinesis;

import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.repo.kinesis.firehose.GlueTableDescriptor;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfig;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfigValidator;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseRecordFormat;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseStreamDescriptor;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class KinesisFirehoseConfigValidatorTest {

	@Mock
	KinesisFirehoseConfig mockConfig;

	@InjectMocks
	KinesisFirehoseConfigValidator validator;

	@Test
	public void testWithNoTables() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
	}

	@Test
	public void testWithTablesRef() {

		GlueTableDescriptor table = new GlueTableDescriptor();

		table.setName("someTableRef");
		table.setColumns(ImmutableMap.of("someColumn", "string"));

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setTableDescriptor(table);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
	}

	@Test(expected = IllegalStateException.class)
	public void testWithEmptyColumnDef() {

		GlueTableDescriptor table = new GlueTableDescriptor();
		table.setName("someTableRef");

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setFormat(KinesisFirehoseRecordFormat.PARQUET);
		stream.setTableDescriptor(table);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
	}

	@Test(expected = IllegalStateException.class)
	public void testWithMissingTableName() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setFormat(KinesisFirehoseRecordFormat.PARQUET);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
	}

	@Test(expected = IllegalStateException.class)
	public void testWithLowBufferInterval() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushInterval(KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL - 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();

	}

	@Test(expected = IllegalStateException.class)
	public void testWithHighBufferInterval() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushInterval(KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL + 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();

	}

	@Test(expected = IllegalStateException.class)
	public void testWithHighBufferSize() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushSize(KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE + 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();

	}

}
