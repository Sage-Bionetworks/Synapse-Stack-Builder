package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.io.File;

public class ElasticBeanstalkExtentionBuilderImpl implements ElasticBeanstalkExtentionBuilder {

	
	CertificateProvider certificateProvider;
	

	@Override
	public File copyWarWithExtensions(File warFile) {
		// Get the certificate information
		CertificateUrls urls = certificateProvider.provideCertificateUrls();
		// TODO: create new war file.
		return warFile;
	}

}
