# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
mvn clean test                                    # Build and run all tests
mvn clean package                                 # Build executable JAR
mvn test -Dtest=S3ConfigValidatorTest              # Run a single test class
mvn test -Dtest=S3ConfigValidatorTest#testValidate # Run a single test method
```

CI runs `mvn --batch-mode clean test` on PRs to `develop` and `release-*` branches.

The build toolchain targets **Java 11** (`pom.xml` compiler source/target). This is distinct from the **deployed** platform image, which is **Java 21 / Tomcat 10** (`repo-defaults.properties`). Do not "align" the pom to 21 — the two versions are intentionally different.

## Architecture

Synapse Stack Builder is a Java application that generates and deploys AWS CloudFormation stacks for the Synapse research platform. The core pipeline is:

**JSON config files → Descriptor objects → Builders → Velocity templates → CloudFormation JSON → AWS deployment**

### Key Patterns

- **Guice DI**: `TemplateGuiceModule` is the central wiring point — all builders, AWS clients, config objects, and Velocity engine are bound here.
- **Velocity templates**: `.vpt` files in `src/main/resources/templates/` are merged with context to produce CloudFormation JSON. The Velocity engine loads templates from both classpath and filesystem.
- **VelocityContextProvider**: Pluggable interface (registered via Guice Multibinder) that contributes variables to template contexts. This is the main extensibility point — new resource types add a provider implementation and bind it in the module.
- **Descriptor/Builder pattern**: Resources are defined as descriptor objects (e.g., `EnvironmentDescriptor`, `S3BucketDescriptor`) with builder-style methods, then transformed into CloudFormation resources via templates.
- **Configuration-driven**: JSON config files (`src/main/resources/templates/`) define S3 buckets, SNS/SQS queues, Kinesis streams, AppConfig, Athena queries, etc. These are deserialized into typed config objects and validated by corresponding `*Validator` classes.

### Stack Deployment Order

Stacks are deployed in dependency order:
1. **Global Resources** — KMS keys, IAM roles, helper Lambdas
2. **VPC** — Network infrastructure, subnets by color (red/blue/green/orange for AZs)
3. **Shared Resources** — RDS databases, encryption, secrets
4. **Environment Stacks** — repo services, workers, tables, migration environments. Each deploys to either **Elastic Beanstalk** or **ECS Fargate**, selected by the `PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS` property (`DeploymentTarget` enum). `RepositoryTemplateBuilderImpl.buildEnvironments()` branches into `buildBeanstalkEnvironments()` or `buildEcsEnvironments()`. See `src/main/java/org/sagebionetworks/template/repo/ecs/CLAUDE.md` for the Fargate path (Docker build → ECR).
5. **CDN & Network** — CloudFront distributions, Network Load Balancers

### Entry Points

Multiple `*Main` classes each build a specific stack type: `RepositoryBuilderMain`, `VpcBuilderMain`, `S3BuilderMain`, `CdnBuilderMain`, `DataWarehouseBuilderMain`, `GlobalResourcesBuilderMain`, etc. `RepositoryBuilderMain` is cross-cutting: it builds S3 buckets (`S3BucketBuilder.buildAllBuckets()`) and deploys docs before building the environment stacks.

## Main Packages (`org.sagebionetworks.template`)

| Package | Purpose |
|---------|---------|
| `repo/` | Repository environment infrastructure (sub-packages: `appconfig/`, `beanstalk/`, `ecs/`, `kinesis/`, `queues/`, `cloudwatchlogs/`, `glue/`, `athena/`, `agent/`, `grid/`) |
| `s3/` | S3 bucket configuration and building |
| `vpc/` | VPC and subnet templates |
| `cdn/` | CloudFront CDN resources |
| `datawarehouse/` | Data warehouse infrastructure and backfill |
| `global/` | Account-level global AWS resources |
| `ip/` | IP address pool management |
| `config/` | Configuration management |
| `cron/` | Scheduled stack teardown / TTL jobs |
| `docs/` | Synapse docs builder |
| `jobs/` | Async admin job executor |
| `markdownit/` | markdown-it Lambda |
| `nlb/` | Network load balancer builder |
| `redirectors/` | User-docs redirector |
| `utils/` | Artifact download helpers |

## Testing

Tests use JUnit 5 with Mockito. Standard pattern:

```java
@ExtendWith(MockitoExtension.class)
public class FooTest {
    @Mock private SomeDependency mockDep;
    @InjectMocks private Foo foo;

    @Test
    public void testSomething() { ... }
}
```

Test files follow `*Test.java` naming. Integration tests use `*IntegrationTest.java`. `RepositoryTemplateBuilderImplTest` is the standard place to assert rendered template shape (prod vs dev) for new resource types.

## Conventions

- **AWS SDK v2 for new code.** New/edited AWS client code uses `software.amazon.awssdk` v2 clients (e.g. `S3Client`, `PutObjectRequest`, `RequestBody`). A v1→v2 migration is in progress, so both are bound in `TemplateGuiceModule` today — do not add new `com.amazonaws...AmazonS3Client` (v1) usage.
- **Adding simple buckets/queues is config-only.** New S3 buckets and SNS/SQS queues are added via config entries (`s3/s3-buckets-config.json`, `repo/sns-and-sqs-config.json`); no Java/builder changes needed for a plain bucket or queue.
- **Two config interfaces — inject the right one.** `RepoConfigurationImpl` auto-loads `templates/repo/repo-defaults.properties` on construction; plain `ConfigurationImpl` loads no defaults. Guice binds them to `RepoConfiguration` vs `Configuration`; repo-scoped builders need `RepoConfiguration` or required defaults are silently absent.
- **`SnsAndSqsConfig` deserializes with Jackson, not GSON** (its class comment says "GSON" but the annotations are `@JsonCreator`/`@JsonProperty`). Don't "fix" the annotations to match the comment. Queue FIFO-ness is inferred purely from a `.fifo` suffix on the queue name.
- **`repo/athena` recurrent queries** require their `destinationQueue` to exist in `SnsAndSqsConfig`, and default their query database to the firehose `GLUE_DB_SUFFIX` constant; the query string is loaded from a file under `athena/` and re-evaluated as a Velocity template.
- **`repo/cloudwatchlogs` config** requires exactly 2 `LogDescriptor`s per `EnvironmentType`, all environment types present, and no duplicate `LogType` — the validator throws otherwise.
- **`docs/` deploys via a custom ETag-diff S3 sync**, gated by a `docs-stack-instance.json` marker with an `instance` counter (only syncs when the configured instance is newer). Do not replace it with a plain `s3 sync`-style copy — it would break the idempotency/versioning gate.

## Subsystem guides

Deeper, package-local conventions live in child `CLAUDE.md` files — read the relevant one before working in that area:
`template/` (top-level — CFN client wrapper & Guice module gotchas), `repo/`, `repo/beanstalk/`, `repo/ecs/`, `repo/agent/`, `repo/grid/`, `repo/kinesis/firehose/`, `s3/`, `vpc/`, `cdn/`, `nlb/`, `global/`, `datawarehouse/`.

## Anti-Patterns — Do NOT

- **Do NOT select multi-AZ subnets positionally / by fixed index** for resources with AZ-specific instance-type constraints (e.g. OpenSearch `VpcOptions.SubnetIds`) — query which AZs support the instance type (EC2 `DescribeInstanceTypeOfferings`) and derive subnet/AZ counts from config. Positional selection silently picks AZs lacking the type and breaks prod (PLFM-9788, PR #901).
- **Do NOT add a service reached over a VPC endpoint (e.g. a new Bedrock service) in `repo/agent` or `repo/grid` without also updating the VPC endpoint list** — see `src/main/java/org/sagebionetworks/template/vpc/CLAUDE.md`; environments silently lose connectivity otherwise.
- **Do NOT create AWS resources dynamically from a Lambda without a CloudFormation Custom Resource to clean them up** — CFN has no knowledge of out-of-band resources, so they accumulate across stack teardown/rebuild (PR #896).
- **Do NOT assume an IAM identity-based Allow grants KMS access** — an explicit `Deny kms:* Principal:*` in a CMK key policy overrides it; granting a new role requires updating the key-policy allowlist itself, and prod vs non-prod branches must be updated separately (PLFM-9782, PR #903).
