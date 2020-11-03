package org.sagebionetworks.template.repo.kinesis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.repo.kinesis.firehose.GlueTableDescriptor;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfig;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfigValidator;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseRecordFormat;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseStreamDescriptor;

import com.google.common.collect.ImmutableMap;

@ExtendWith(MockitoExtension.class)
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

	@Test
	public void testWithEmptyColumnDef() {

		GlueTableDescriptor table = new GlueTableDescriptor();
		table.setName("someTableRef");

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setFormat(KinesisFirehoseRecordFormat.PARQUET);
		stream.setTableDescriptor(table);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		});
	}

	@Test
	public void testWithMissingTableDescriptorForParquet() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setFormat(KinesisFirehoseRecordFormat.PARQUET);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));
		
		assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		});
	}

	@Test
	public void testWithLowBufferInterval() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushInterval(KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL - 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		});

	}

	@Test
	public void testWithHighBufferInterval() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushInterval(KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL + 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		});

	}

	@Test
	public void testWithLowBufferSize() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushSize(KinesisFirehoseStreamDescriptor.MIN_BUFFER_SIZE - 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		});

	}

	@Test
	public void testWithHighBufferSize() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setBufferFlushSize(KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE + 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		});

	}

}
