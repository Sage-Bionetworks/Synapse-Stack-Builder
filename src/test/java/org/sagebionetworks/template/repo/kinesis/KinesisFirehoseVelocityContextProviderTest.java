package org.sagebionetworks.template.repo.kinesis;

import com.google.common.collect.ImmutableSet;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfig;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseStreamDescriptor;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseVelocityContextProvider;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.KINESIS_FIREHOSE_BUCKETS;
import static org.sagebionetworks.template.Constants.KINESIS_FIREHOSE_STREAM_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseVelocityContextProvider.GLUE_DB_SUFFIX;

@ExtendWith(MockitoExtension.class)
public class KinesisFirehoseVelocityContextProviderTest {

	@Mock
	VelocityContext mockContext;

	@Mock
	RepoConfiguration mockRepoConfig;

	@Mock
	KinesisFirehoseConfig mockConfig;

	@Mock
	KinesisFirehoseStreamDescriptor mockStream;
	
	@Mock
	KinesisFirehoseStreamDescriptor mockStream2;

	@Mock
	GlueTableDescriptor mockTable;

	@InjectMocks
	KinesisFirehoseVelocityContextProvider contextProvider;

	private String testStack = "TestStack";
	private String testInstance = "TestInstance";
	private Set<KinesisFirehoseStreamDescriptor> testStreams;
	
	@BeforeEach
	public void before() {
		testStreams = Collections.singleton(mockStream);
		when(mockStream.getBucket()).thenReturn(KinesisFirehoseStreamDescriptor.DEFAULT_BUCKET);
		when(mockRepoConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(testStack);
		when(mockRepoConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(testInstance);
		when(mockStream.getTableDescriptor()).thenReturn(mockTable);
		when(mockConfig.getStreamDescriptors()).thenReturn(testStreams);
	}

	@Test
	public void testAddToContext() {
		
		contextProvider.addToContext(mockContext);
		
		verify(mockContext).put(GLUE_DATABASE_NAME, (testStack + testInstance + GLUE_DB_SUFFIX));
		verify(mockContext).put(KINESIS_FIREHOSE_STREAM_DESCRIPTORS, testStreams);
		verify(mockContext).put(KINESIS_FIREHOSE_BUCKETS, testStreams.stream().map(KinesisFirehoseStreamDescriptor::getBucket).collect(Collectors.toSet()));
	}
	
	@Test
	public void testAddToContextWithoutDevOnlyStreamsInProd() {
		String prodStack = "Prod";
		
		when(mockRepoConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(prodStack);
		when(mockStream.isDevOnly()).thenReturn(true);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockContext).put(KINESIS_FIREHOSE_STREAM_DESCRIPTORS, Collections.emptySet());
	}
	
	@Test
	public void testAddToContextWithDevOnlyStreams() {
		
		// A stack different than prod
		String devStack = "someOtherStack";
		
		when(mockRepoConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(devStack);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockContext).put(KINESIS_FIREHOSE_STREAM_DESCRIPTORS, Collections.singleton(mockStream));
	}
	
	@Test
	public void testAddToContextWithoutDevOnlyStreamsMixed() {
		
		String prodStack = "prod";
		
		// Second stream is dev only
		when(mockStream2.isDevOnly()).thenReturn(true);
		
		Set<KinesisFirehoseStreamDescriptor> streams = ImmutableSet.of(mockStream, mockStream2);
		
		when(mockConfig.getStreamDescriptors()).thenReturn(streams);
		when(mockRepoConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(prodStack);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockContext).put(KINESIS_FIREHOSE_STREAM_DESCRIPTORS, Collections.singleton(mockStream));
	}
	
	@Test
	public void testAddToContextWithParameterizedTableName() {
		
		String originalTableName = "TestTable";
		
		when(mockTable.getName()).thenReturn(originalTableName);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockTable).setName((testStack + testInstance + originalTableName));
	}
	
	@Test
	public void testAddToContextWithDefaultBucket() {
		
		String originalTableName = "TestTable";
		
		when(mockTable.getName()).thenReturn(originalTableName);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockStream).setBucket(TemplateUtils.replaceStackVariable(KinesisFirehoseStreamDescriptor.DEFAULT_BUCKET, testStack));
	}
	
	@Test
	public void testAddToContextWithCustomBucket() {
		
		String originalTableName = "TestTable";
		String customBucket = "customBucket";
		
		when(mockStream.getBucket()).thenReturn(customBucket);
		when(mockTable.getName()).thenReturn(originalTableName);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockStream).setBucket(customBucket);
	}
	
	@Test
	public void testAddToContextWithCustomStackBucket() {
		
		String originalTableName = "TestTable";
		String customBucket = "${stack}.customBucket";
		
		when(mockStream.getBucket()).thenReturn(customBucket);
		when(mockTable.getName()).thenReturn(originalTableName);
		
		// Call under test
		contextProvider.addToContext(mockContext);
		
		verify(mockStream).setBucket(TemplateUtils.replaceStackVariable(customBucket, testStack));
	}
}
