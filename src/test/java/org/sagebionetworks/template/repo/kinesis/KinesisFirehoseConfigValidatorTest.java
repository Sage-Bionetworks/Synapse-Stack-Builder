package org.sagebionetworks.template.repo.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	
	private static final String STREAM_NAME = "someStream";

	@Test
	public void testWithNoTables() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
	}

	@Test
	public void testWithTablesRef() {

		GlueTableDescriptor table = new GlueTableDescriptor();

		table.setName("someTableRef");
		table.setColumns(ImmutableMap.of("someColumn", "string"));

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setTableDescriptor(table);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
	}
	
	@Test
	public void testWithEmptyName() {
		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(" ");
		
		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();
		
		assertEquals("The stream name cannot be empty", errorMessage);
	}

	@Test
	public void testWithEmptyColumnDef() {

		GlueTableDescriptor table = new GlueTableDescriptor();
		table.setName("someTableRef");

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setFormat(KinesisFirehoseRecordFormat.PARQUET);
		stream.setTableDescriptor(table);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();
		
		assertEquals("Stream someStream: no column definition found for table someTableRef", errorMessage);
	}

	@Test
	public void testWithMissingTableDescriptorForParquet() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setFormat(KinesisFirehoseRecordFormat.PARQUET);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();
		
		assertEquals("The stream someStream is configured to be converted to parquet records, but the tableDescriptor is missing", errorMessage);
	}

	@Test
	public void testWithLowBufferInterval() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setBufferFlushInterval(KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL - 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();

		assertEquals("Stream someStream: the minimum value for the bufferFlushInterval is 60 (was 59)", errorMessage);
	}

	@Test
	public void testWithHighBufferInterval() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setBufferFlushInterval(KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL + 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();

		assertEquals("Stream someStream: the maximum value for the bufferFlushInterval is 900 (was 901)", errorMessage);
	}

	@Test
	public void testWithLowBufferSize() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setBufferFlushSize(KinesisFirehoseStreamDescriptor.MIN_BUFFER_SIZE - 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();

		assertEquals("Stream someStream: the minimum value for the bufferFlushSize is 64 (was 63)", errorMessage);
	}

	@Test
	public void testWithHighBufferSize() {

		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setBufferFlushSize(KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE + 1);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();

		assertEquals("Stream someStream: the maximum value for the bufferFlushSize is 128 (was 129)", errorMessage);
	}
	
	@Test
	public void testWithEmptyBucket() {
		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);
		stream.setBucket(" ");
		

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			validator.validate();
		}).getMessage();
		
		assertEquals("Stream someStream: the bucket cannot be empty", errorMessage);
	}
	
	@Test
	public void testWithDefaultBucket() {
		KinesisFirehoseStreamDescriptor stream = new KinesisFirehoseStreamDescriptor();
		stream.setName(STREAM_NAME);

		when(mockConfig.getStreamDescriptors()).thenReturn(Collections.singleton(stream));

		validator.validate();
		
		assertEquals(KinesisFirehoseStreamDescriptor.DEFAULT_BUCKET, stream.getBucket());
	}

}
