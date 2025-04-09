package org.sagebionetworks.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.Configuration;

import software.amazon.awssdk.services.imagebuilder.ImagebuilderClient;
import software.amazon.awssdk.services.imagebuilder.model.Ami;
import software.amazon.awssdk.services.imagebuilder.model.ImageState;
import software.amazon.awssdk.services.imagebuilder.model.ImageStatus;
import software.amazon.awssdk.services.imagebuilder.model.ImageSummary;
import software.amazon.awssdk.services.imagebuilder.model.ListImagePipelineImagesRequest;
import software.amazon.awssdk.services.imagebuilder.model.ListImagePipelineImagesResponse;
import software.amazon.awssdk.services.imagebuilder.model.OutputResources;

@ExtendWith(MockitoExtension.class)
class ImageBuilderClientImplTest {
	
	@Mock
	ImagebuilderClient mockImagebuilderClient;
	@Mock
	Configuration mockConfig;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;

	private ListImagePipelineImagesRequest listImagesRequest;
	private ListImagePipelineImagesResponse listImagesResult;
	
	private String IMAGE_PIPELINE_ARN="arn:aws:imagebuilder:us-east-1:867686887310:image/cis-for-eb";;
	private String IMAGE_ID= "ami-0123456789";
	
	private ImageBuilderClient imageBuilderClient;

	@BeforeEach
	void setUp() throws Exception {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		imageBuilderClient = new ImageBuilderClientImpl(mockImagebuilderClient, mockConfig, mockLoggerFactory);
		listImagesRequest = ListImagePipelineImagesRequest.builder().imagePipelineArn(IMAGE_PIPELINE_ARN).build();
		listImagesResult = ListImagePipelineImagesResponse.builder().
				imageSummaryList(
						ImageSummary.builder().
						dateCreated("2025-04-05T23:39:43.900Z").
						state(ImageState.builder().status(ImageStatus.AVAILABLE).build()).
						outputResources(OutputResources.builder().
								amis(Ami.builder().image(IMAGE_ID).build()).
								build())
				.build()

		).build();
	}

	@Test
	public void testGetLatestImageIdForImagePipelineArn() {
		// happy case: just one page of results, one image, image is Available
		when(mockImagebuilderClient.listImagePipelineImages(listImagesRequest)).thenReturn(listImagesResult);
		
		// method under test
		String actualImageId = imageBuilderClient.getLatestImageIdForImagePipelineArn(IMAGE_PIPELINE_ARN);
		
		assertEquals(IMAGE_ID, actualImageId);
	}

	@Test
	public void testGetLatestImageIdForImagePipelineArn_complex() {
		// two pages
		// first page has an available image
		// first page has a broken image with a later date
		// second page has an available image with an earlier date
		
		listImagesRequest = ListImagePipelineImagesRequest.builder().imagePipelineArn(IMAGE_PIPELINE_ARN).build();
		
		String nextPageToken = "nextPage";
		
		ImageSummary image1 = ImageSummary.builder().
				dateCreated("2025-04-05T23:39:43.900Z").
				state(ImageState.builder().
				status(ImageStatus.AVAILABLE).build()).
				outputResources(OutputResources.builder().
						amis(Ami.builder().image(IMAGE_ID).build()).
						build()).build();
		
		
		ImageSummary image2 = ImageSummary.builder().
				dateCreated("2025-04-06T23:39:43.900Z").
				state(ImageState.builder().
				status(ImageStatus.CANCELLED).build()).
				outputResources(OutputResources.builder().
						amis(Ami.builder().image("ami-cancelled").build()).
						build()).build();
		
		listImagesResult = ListImagePipelineImagesResponse.builder().
				nextToken(nextPageToken).
				imageSummaryList(image1, image2).build();
		
		ListImagePipelineImagesRequest listImagesRequestPage2 = 
				ListImagePipelineImagesRequest.builder().imagePipelineArn(IMAGE_PIPELINE_ARN).nextToken(nextPageToken).build();
		
		ListImagePipelineImagesResponse listImagesResultPage2 =  
				ListImagePipelineImagesResponse.builder().
				imageSummaryList(ImageSummary.builder().
						dateCreated("2025-04-01T23:39:43.900Z").
						state(ImageState.builder().
						status(ImageStatus.AVAILABLE).build()).
						outputResources(OutputResources.builder().
								amis(Ami.builder().image("ami-old-image").build()).
								build()).build()).build();


		when(mockImagebuilderClient.listImagePipelineImages(listImagesRequest)).thenReturn(listImagesResult);
		when(mockImagebuilderClient.listImagePipelineImages(listImagesRequestPage2)).thenReturn(listImagesResultPage2);
		
		// method under test
		String actualImageId = imageBuilderClient.getLatestImageIdForImagePipelineArn(IMAGE_PIPELINE_ARN);
		
		// method should return the first image, since it's the latest Available image
		assertEquals(IMAGE_ID, actualImageId);
	}


}
