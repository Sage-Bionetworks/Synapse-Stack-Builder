package org.sagebionetworks.template.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import com.amazonaws.services.s3.AmazonS3;

@ExtendWith(MockitoExtension.class)
public class AgentBuilderImplTest {

	@Captor
	ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;
	@Mock
	private CloudFormationClient cloudFormationClient;
	private VelocityEngine velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
	@Mock
	private Configuration mockConfig;
	@Mock
	private Logger logger;
	@Mock
	private StackTagsProvider tagsProvider;
	@Mock
	private LoggerFactory loggerFactory;
	@Mock
	private ArtifactDownload mockDownloader;
	@Mock
	private AmazonS3 mockS3Client;

	@Mock
	private File mockLambdaFile;
	@Mock
	private File mockLayerFile;

	private AgentBuilderImpl agentBuilder;
	private AgentBuilderImpl agentBuilderSpy;

	@BeforeEach
	public void before() {
		when(loggerFactory.getLogger(any())).thenReturn(logger);
		agentBuilder = new AgentBuilderImpl(cloudFormationClient, velocityEngine, mockConfig, loggerFactory,
				mockDownloader, mockS3Client, tagsProvider);
		agentBuilderSpy = Mockito.spy(agentBuilder);
	}

	@Test
	public void testBuildAndDeploy() throws IOException {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn("test");
		when(mockConfig.getProperty(PROPERTY_KEY_ARTIFACT_VERSION)).thenReturn("v0.0.0");

		when(mockDownloader.downloadFile(
				"https://github.com/Sage-Bionetworks/Synapse-Agent-Lambda/releases/download/v0.0.0/lambda-develop-SNAPSHOT.jar"))
				.thenReturn(mockLambdaFile);;
		
		doReturn("layers-key").when(agentBuilderSpy).createAndUploadLayerZip(any(),any(),any());
		// call under test
		agentBuilderSpy.buildAndDeploy();
	}

}
