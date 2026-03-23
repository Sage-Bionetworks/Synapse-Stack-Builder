package org.sagebionetworks.template.repo.ecs;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

/**
 * Description of an ECS Fargate environment.
 */
public class EcsEnvironmentDescriptor {

	String name;
	String refName;
	int number;
	EnvironmentType type;
	String dockerImageUri;
	String healthCheckUrl;
	int minTasks;
	int maxTasks;
	int cpu;
	int memory;
	int containerPort;
	String sslCertificateARN;
	String hostedZone;
	SourceBundle secretsSource;

	public String getName() {
		return name;
	}

	public String getRefName() {
		return refName;
	}

	public int getNumber() {
		return number;
	}

	public String getType() {
		return type != null ? type.getShortName() : null;
	}

	public EnvironmentType getEnvironmentType() {
		return type;
	}

	public String getInstanceProfileSuffix() {
		return type != null ? type.getInstanceProfileSuffix() : null;
	}

	public String getDockerImageUri() {
		return dockerImageUri;
	}

	public String getHealthCheckUrl() {
		return healthCheckUrl;
	}

	public int getMinTasks() {
		return minTasks;
	}

	public int getMaxTasks() {
		return maxTasks;
	}

	public int getCpu() {
		return cpu;
	}

	public int getMemory() {
		return memory;
	}

	public int getContainerPort() {
		return containerPort;
	}

	public String getSslCertificateARN() {
		return sslCertificateARN;
	}

	public String getHostedZone() {
		return hostedZone;
	}

	public SourceBundle getSecretsSource() {
		return secretsSource;
	}

	/**
	 * Is the type repository or workers?
	 */
	public boolean isTypeRepositoryOrWorkers() {
		return EnvironmentType.REPOSITORY_SERVICES.equals(type) || EnvironmentType.REPOSITORY_WORKERS.equals(type);
	}

	public EcsEnvironmentDescriptor withName(String name) {
		this.name = name;
		return this;
	}

	public EcsEnvironmentDescriptor withRefName(String refName) {
		this.refName = refName;
		return this;
	}

	public EcsEnvironmentDescriptor withNumber(int number) {
		this.number = number;
		return this;
	}

	public EcsEnvironmentDescriptor withType(EnvironmentType type) {
		this.type = type;
		return this;
	}

	public EcsEnvironmentDescriptor withDockerImageUri(String dockerImageUri) {
		this.dockerImageUri = dockerImageUri;
		return this;
	}

	public EcsEnvironmentDescriptor withHealthCheckUrl(String healthCheckUrl) {
		this.healthCheckUrl = healthCheckUrl;
		return this;
	}

	public EcsEnvironmentDescriptor withMinTasks(int minTasks) {
		this.minTasks = minTasks;
		return this;
	}

	public EcsEnvironmentDescriptor withMaxTasks(int maxTasks) {
		this.maxTasks = maxTasks;
		return this;
	}

	public EcsEnvironmentDescriptor withCpu(int cpu) {
		this.cpu = cpu;
		return this;
	}

	public EcsEnvironmentDescriptor withMemory(int memory) {
		this.memory = memory;
		return this;
	}

	public EcsEnvironmentDescriptor withContainerPort(int containerPort) {
		this.containerPort = containerPort;
		return this;
	}

	public EcsEnvironmentDescriptor withSslCertificateARN(String sslCertificateARN) {
		this.sslCertificateARN = sslCertificateARN;
		return this;
	}

	public EcsEnvironmentDescriptor withHostedZone(String hostedZone) {
		this.hostedZone = hostedZone;
		return this;
	}

	public EcsEnvironmentDescriptor withSecretsSource(SourceBundle secretsSource) {
		this.secretsSource = secretsSource;
		return this;
	}

	@Override
	public String toString() {
		return "EcsEnvironmentDescriptor [name=" + name + ", refName=" + refName + ", number=" + number + ", type="
				+ type + ", dockerImageUri=" + dockerImageUri + ", healthCheckUrl=" + healthCheckUrl + ", minTasks="
				+ minTasks + ", maxTasks=" + maxTasks + ", cpu=" + cpu + ", memory=" + memory + ", containerPort="
				+ containerPort + ", sslCertificateARN=" + sslCertificateARN + ", hostedZone=" + hostedZone
				+ ", secretsSource=" + secretsSource + "]";
	}
}
