# CLAUDE.md — `global` (incl. `waitconditions`)

Builds account-level global resources (KMS, IAM, Cognito, SES, Bedrock knowledge base). `GlobalResourcesBuilderImpl` deploys the CFN stack, then does imperative AWS-API post-processing that CloudFormation cannot express.

## Post-CFN imperative steps (`GlobalResourcesBuilderImpl`)

- **Cognito → Secrets Manager copy.** After the stack completes, `copyCognitoSecretsToSecretsManager` reads the Cognito app client id/secret via the Cognito API and writes them to Secrets Manager under `{stackPrefix}.{KEY}` (also stores the OIDC discovery-doc URL). `setSecret` is create-or-`putSecretValue` idempotent. Do not move this into the CFN template — CFN can't retrieve the app secret.
- **SES topic wiring is prod-only** — gated by `"prod".equalsIgnoreCase(stack)` (a raw string compare, not `Constants.isProd`). Match the existing check if you add similar prod-gating here.
- **Route 53 query logging is prod-only and covers every public hosted zone** (PLFM-9962). The zones aren't CFN-managed, so the template only creates `${stack}Route53QueryLogGroup` plus an `AWS::Logs::ResourcePolicy` for `route53.amazonaws.com`. After that, `enableQueryLoggingOnPublicHostedZones` lists the account's public zones and calls `CreateQueryLoggingConfig` on any zone that doesn't have one yet. Gotchas:
  - The `Route53QueryLogGroupArn` output deliberately has no trailing `:*` (`GetAtt Arn` includes it, and Route 53 rejects that).
  - The resource policy's `PolicyDocument` is a string built with `Fn::Join`, because `Fn::Sub`'s `${...}` collides with Velocity.
  - The log group must stay `Retain` on prod, because zone configs outside CFN point at it.

## WaitConditionHandler (`waitconditions/`)

`WaitConditionHandler` implementations are a **second Guice `Multibinder` extension point** (parallel to `VelocityContextProvider`), bound in `TemplateGuiceModule`.

- **Each handler is matched to a CFN template resource by exact string** via `getWaitConditionId()` — it must equal the wait-condition logical id in the `.vpt` template, or the handler never fires.
- **Handlers must be idempotent** — they are invoked repeatedly while the outer stack poll runs (e.g. `SynapseHelpKnowledgeBaseDataSourceSync` returns early if an ingestion job already exists).
- **`SynapseHelpKnowledgeBaseDataSourceSync.handle()` runs its own blocking `Thread.sleep(10s)` poll loop** *inside* the outer `CloudFormationClientWrapperImpl.waitForStackToComplete` loop. This nested blocking counts against the outer wait timeout — keep handler work bounded.
