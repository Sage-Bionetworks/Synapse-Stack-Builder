package org.sagebionetworks.template.repo.ecs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.ssl.CertificateBuilder;
import org.sagebionetworks.template.repo.beanstalk.ssl.CertificatePair;
import org.sagebionetworks.template.utils.ArtifactDownload;

import com.google.inject.Inject;

import static org.sagebionetworks.template.Constants.ECS_JVM_MEMORY_FRACTION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_CONTAINER_PORT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_TASK_MEMORY;

public class DockerImageBuilderImpl implements DockerImageBuilder {

	static final String TEMPLATE_DOCKERFILE = "templates/repo/ecs/Dockerfile.vpt";
	static final String TEMPLATE_SERVER_XML = "templates/repo/ecs/server.xml.vpt";
	static final String TEMPLATE_STARTUP_SH = "templates/repo/ecs/startup.sh.vpt";

	private final ArtifactDownload downloader;
	private final CertificateBuilder certificateBuilder;
	private final VelocityEngine velocityEngine;
	private final RepoConfiguration config;
	private final Logger logger;

	@Inject
	public DockerImageBuilderImpl(ArtifactDownload downloader, CertificateBuilder certificateBuilder,
			VelocityEngine velocityEngine, RepoConfiguration config, LoggerFactory loggerFactory) {
		this.downloader = downloader;
		this.certificateBuilder = certificateBuilder;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.logger = loggerFactory.getLogger(DockerImageBuilderImpl.class);
	}

	@Override
	public String buildAndPushImage(EnvironmentType environment, String version, int number, String stack, String instance) {
		String artifactoryUrl = environment.createArtifactoryUrl(version);
		logger.info("Downloading artifact for Docker image: " + artifactoryUrl);
		File warFile = downloader.downloadFile(artifactoryUrl);
		File buildContext = null;
		try {
			buildContext = createBuildContext(warFile, environment, version, number);
			String imageTag = stack + "-" + instance + "-" + version + "-" + number;
			String ecrRepoName = getEcrRepositoryName(environment, stack);

			// Get the AWS account ID and region for the ECR URI
			String accountId = getAwsAccountId();
			String region = "us-east-1";
			String imageUri = accountId + ".dkr.ecr." + region + ".amazonaws.com/" + ecrRepoName + ":" + imageTag;

			logger.info("Building Docker image: " + imageUri);
			dockerBuild(buildContext, imageUri);

			logger.info("Pushing Docker image: " + imageUri);
			ecrLogin(accountId, region);
			dockerPush(imageUri);

			return imageUri;
		} finally {
			warFile.delete();
			if (buildContext != null) {
				deleteDirectory(buildContext);
			}
		}
	}

	/**
	 * Create the Docker build context directory with Dockerfile, server.xml, WAR, and cert files.
	 */
	File createBuildContext(File warFile, EnvironmentType environment, String version, int number) {
		try {
			File buildDir = Files.createTempDirectory("docker-build-").toFile();

			// Generate self-signed TLS certificate
			CertificatePair certPair = certificateBuilder.buildNewX509CertificatePair();

			// Write certificate and key files
			File serverCrt = new File(buildDir, "server.crt");
			File serverKey = new File(buildDir, "server.key");
			writeFile(serverCrt, certPair.getX509CertificatePEM());
			writeFile(serverKey, certPair.getPrivateKeyPEM());
			
			// create the server.keystore file from the server.crt and server.key files
			File serverKeystore = new File(buildDir, "server.keystore");
			executeCommand(
				"yum install -y openssl",
				"&& yum clean all",
				"&& openssl pkcs12 -export",
				"-in", serverCrt.getAbsolutePath(),
				"-inkey", serverKey.getAbsolutePath(),
				"-out", serverKeystore.getAbsolutePath(),
				"-name tomcat",
				"-passout pass:changeit"
			);

			// Copy WAR file
			String s3Key = environment.createS3Key(version, number);
			String warFileName = s3Key.substring(s3Key.lastIndexOf('/') + 1);
			File destWar = new File(buildDir, warFileName);
			Files.copy(warFile.toPath(), destWar.toPath());

			// Render server.xml template
			int containerPort = config.getIntegerProperty(PROPERTY_KEY_ECS_CONTAINER_PORT);
			VelocityContext context = new VelocityContext();
			context.put("containerPort", containerPort);
			context.put("warFileName", warFileName);
			renderTemplate(TEMPLATE_SERVER_XML, context, new File(buildDir, "server.xml"));

			// Render startup script (converts env vars to JVM -D flags)
			renderTemplate(TEMPLATE_STARTUP_SH, context, new File(buildDir, "startup.sh"));

			// Render Dockerfile template
			int taskMemory = config.getIntegerProperty(PROPERTY_KEY_ECS_TASK_MEMORY);
			int jvmMemory = (int) (taskMemory * ECS_JVM_MEMORY_FRACTION);
			context.put("jvmMemory", jvmMemory);
			renderTemplate(TEMPLATE_DOCKERFILE, context, new File(buildDir, "Dockerfile"));

			return buildDir;
		} catch (IOException e) {
			throw new RuntimeException("Failed to create Docker build context", e);
		}
	}

	void renderTemplate(String templatePath, VelocityContext context, File outputFile) {
		Template template = velocityEngine.getTemplate(templatePath);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		writeFile(outputFile, writer.toString());
	}

	String getEcrRepositoryName(EnvironmentType environment, String stack) {
		return stack+"-synapse-" + environment.getShortName();
	}

	String getAwsAccountId() {
		try {
			ProcessBuilder pb = new ProcessBuilder("aws", "sts", "get-caller-identity", "--query", "Account", "--output", "text");
			pb.redirectErrorStream(true);
			Process process = pb.start();
			String output = new String(process.getInputStream().readAllBytes()).trim();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new RuntimeException("Failed to get AWS account ID: " + output);
			}
			return output;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to get AWS account ID", e);
		}
	}

	void ecrLogin(String accountId, String region) {
		// Since we can't avoid shelling out for Docker operations anyway, switching just the ECR auth 
		// to a Java client would add complexity without eliminating the CLI dependency.
		executeCommand("aws", "ecr", "get-login-password", "--region", region,
				"|", "docker", "login", "--username", "AWS", "--password-stdin",
				accountId + ".dkr.ecr." + region + ".amazonaws.com");
	}

	void dockerBuild(File buildContext, String imageUri) {
		executeCommand("docker", "build", "-t", imageUri, buildContext.getAbsolutePath());
	}

	void dockerPush(String imageUri) {
		executeCommand("docker", "push", imageUri);
	}

	void executeCommand(String... command) {
		try {
			// For piped commands, use shell
			String commandStr = String.join(" ", command);
			ProcessBuilder pb = new ProcessBuilder("sh", "-c", commandStr);
			pb.inheritIO();
			Process process = pb.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new RuntimeException("Command failed with exit code " + exitCode + ": " + commandStr);
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to execute command: " + String.join(" ", command), e);
		}
	}

	static void writeFile(File file, String content) {
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(content);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write file: " + file.getAbsolutePath(), e);
		}
	}

	static void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteDirectory(child);
				}
			}
		}
		dir.delete();
	}
}
