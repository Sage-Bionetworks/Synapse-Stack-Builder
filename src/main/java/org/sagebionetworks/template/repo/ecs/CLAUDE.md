# CLAUDE.md — `repo/ecs`

ECS Fargate deployment target for repository environments, an alternative to Elastic Beanstalk (selected by `PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS`). Unlike the rest of the codebase, this package does NOT go through the Velocity → CloudFormation pipeline for the image: `DockerImageBuilderImpl` builds a Docker image locally and pushes it to ECR, then `ecs-fargate-template.json.vpt` references the resulting image URI.

## Flow

`buildAndPushImage()`: download WAR from Artifactory → render a build context (Dockerfile, server.xml, startup.sh, nginx.conf from `.vpt` templates + a self-signed TLS cert) → `docker buildx build` → `aws ecr` login → `docker push`. Returns the image URI consumed by the Fargate template.

## Constraints

- **Docker build MUST stay `docker buildx build --platform linux/arm64`** — the Fargate task definition declares `runtimePlatform` ARM64 regardless of the build host's architecture (`DockerImageBuilderImpl.dockerBuild`). Changing the platform produces an image the task cannot run.
- **Docker / `aws sts` / `aws ecr` are invoked by shelling out** via `ProcessBuilder` (`sh -c` for piped commands), not the Java AWS SDK. This is deliberate — since Docker itself must be shelled out to, switching just ECR auth to a Java client adds complexity without removing the CLI dependency (comment in `ecrLogin`). The AWS CLI and Docker with buildx must be present on the build host.
- **Region is hardcoded `us-east-1`.** Image URI format is `{account}.dkr.ecr.{region}.amazonaws.com/{ecrRepoName}:{tag}`; tag is `{stack}-{instance}-{version}-{number}`; ECR repo name is `{stack}-synapse-{shortName}` (`getEcrRepositoryName`). The ECR repositories themselves are created by `global/ecr-repositories.json.vpt`.
- **Temp WAR file and build-context dir are deleted in `finally`** (`buildAndPushImage`) — preserve this cleanup when editing.

## Gotcha

- `EcsEnvironmentDescriptor.getType()` returns `type.getShortName()`, **not** the enum name. Use `getEnvironmentType()` to get the `EnvironmentType` enum. Do not "fix" `getType()` to return the enum name — templates depend on the short name.
