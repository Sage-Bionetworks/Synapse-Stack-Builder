package org.sagebionetworks.template;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;

import com.google.inject.Inject;

import software.amazon.awssdk.services.imagebuilder.model.Ami;
import software.amazon.awssdk.services.imagebuilder.model.ImageStatus;
import software.amazon.awssdk.services.imagebuilder.model.ImageSummary;
import software.amazon.awssdk.services.imagebuilder.model.ListImagePipelineImagesRequest;
import software.amazon.awssdk.services.imagebuilder.model.ListImagePipelineImagesResponse;

public class ImageBuilderClientImpl implements ImageBuilderClient {
	
	software.amazon.awssdk.services.imagebuilder.ImagebuilderClient imageBuilder;
	Configuration configuration;
	Logger logger;

	@Inject
	public ImageBuilderClientImpl(software.amazon.awssdk.services.imagebuilder.ImagebuilderClient ib, Configuration configuration, LoggerFactory loggerFactory) {
		this.imageBuilder = ib;
		this.configuration = configuration;
		this.logger = loggerFactory.getLogger(ImageBuilderClientImpl.class);
	}

	@Override
	public String getLatestImageIdForImagePipelineArn(String imagePipelineArn) {
		if(imagePipelineArn == null) {
			return null;
		}
		String latestDate = null;
		String latestImage = null;
		String nextPageToken = null;
		while (true) {
			ListImagePipelineImagesRequest listImagePipelineImagesRequest = 
					ListImagePipelineImagesRequest.builder().
					imagePipelineArn(imagePipelineArn).nextToken(nextPageToken).build();
			ListImagePipelineImagesResponse result = imageBuilder.listImagePipelineImages(listImagePipelineImagesRequest);

			for (ImageSummary imageSummary : result.imageSummaryList()) {
				if (!ImageStatus.AVAILABLE.equals(imageSummary.state().status())) {
					logger.info("Found unavailable image for "+imageSummary.name());
					continue;
				}
				List<Ami> amis = imageSummary.outputResources().amis();
				// we know the build pipeline creates just one AMI
				if (amis.size()!=1) {
					String message="Expected one AMI but found "+amis.size();
					logger.error(message);
					throw new IllegalStateException(message);
				}
				String amiId=amis.get(0).image();
				if (latestDate == null || latestDate.compareTo(imageSummary.dateCreated())<0) {
					latestImage = amiId;
					latestDate = imageSummary.dateCreated();
				}
			}
			nextPageToken = result.nextToken();
			if (nextPageToken==null) {
				break;
			}
		}
		return latestImage;
	}
}
