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

## Architecture

Synapse Stack Builder is a Java application that generates and deploys AWS CloudFormation stacks for the Synapse research platform. The core pipeline is:

**JSON config files → Descriptor objects → Builders → Velocity templates → CloudFormation JSON → AWS deployment**

### Key Patterns

- **Guice DI**: `TemplateGuiceModule` is the central wiring point — all builders, AWS clients, config objects, and Velocity engine are bound here.
- **Velocity templates**: `.vtp` files in `src/main/resources/templates/` are merged with context to produce CloudFormation JSON. The Velocity engine loads templates from both classpath and filesystem.
- **VelocityContextProvider**: Pluggable interface (registered via Guice Multibinder) that contributes variables to template contexts. This is the main extensibility point — new resource types add a provider implementation and bind it in the module.
- **Descriptor/Builder pattern**: Resources are defined as descriptor objects (e.g., `EnvironmentDescriptor`, `S3BucketDescriptor`) with builder-style methods, then transformed into CloudFormation resources via templates.
- **Configuration-driven**: JSON config files (`src/main/resources/templates/`) define S3 buckets, SNS/SQS queues, Kinesis streams, AppConfig, Athena queries, etc. These are deserialized into typed config objects and validated by corresponding `*Validator` classes.

### Stack Deployment Order

Stacks are deployed in dependency order:
1. **Global Resources** — KMS keys, IAM roles, helper Lambdas
2. **VPC** — Network infrastructure, subnets by color (red/blue/green/orange for AZs)
3. **Shared Resources** — RDS databases, encryption, secrets
4. **Environment Stacks** — Elastic Beanstalk environments (repo services, workers, tables, migration)
5. **CDN & Network** — CloudFront distributions, Network Load Balancers

### Entry Points

Multiple `*Main` classes each build a specific stack type: `RepositoryBuilderMain`, `VpcBuilderMain`, `S3BuilderMain`, `CdnBuilderMain`, `DataWarehouseBuilderMain`, `GlobalResourcesBuilderMain`, etc.

## Main Packages (`org.sagebionetworks.template`)

| Package | Purpose |
|---------|---------|
| `repo/` | Repository/Elastic Beanstalk infrastructure (sub-packages: `appconfig/`, `beanstalk/`, `kinesis/`, `queues/`, `cloudwatchlogs/`, `glue/`, `athena/`, `agent/`) |
| `s3/` | S3 bucket configuration and building |
| `vpc/` | VPC and subnet templates |
| `cdn/` | CloudFront CDN resources |
| `datawarehouse/` | Data warehouse infrastructure and backfill |
| `global/` | Account-level global AWS resources |
| `ip/` | IP address pool management |
| `config/` | Configuration management |

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

Test files follow `*Test.java` naming. Integration tests use `*IntegrationTest.java`.
