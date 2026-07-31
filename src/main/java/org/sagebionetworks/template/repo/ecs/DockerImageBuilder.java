package org.sagebionetworks.template.repo.ecs;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

/**
 * Builds Docker images for ECS Fargate deployment.
 * Downloads the base WAR from Artifactory, generates a self-signed TLS certificate,
 * builds a Docker image with Corretto + Tomcat + WAR + cert, and pushes to ECR.
 */
public interface DockerImageBuilder {

	/**
	 * Build and push a Docker image for the given environment type.
	 *
	 * @param environment the environment type (repo, workers, portal)
	 * @param version the application version
	 * @param number the environment number
	 * @param stack the stack name (e.g., "dev", "prod")
	 * @param instance the stack instance (e.g., "101")
	 * @return the Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/synapse-repo:dev-101-v582-3)
	 */
	String buildAndPushImage(EnvironmentType environment, String version, int number, String stack, String instance);
}
