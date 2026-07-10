# CLAUDE.md — `global` (incl. `waitconditions`)

Builds account-level global resources (KMS, IAM, Cognito, SES, Bedrock knowledge base). `GlobalResourcesBuilderImpl` deploys the CFN stack, then does imperative AWS-API post-processing that CloudFormation cannot express.

## Post-CFN imperative steps (`GlobalResourcesBuilderImpl`)

- **Cognito → Secrets Manager copy.** After the stack completes, `copyCognitoSecretsToSecretsManager` reads the Cognito app client id/secret via the Cognito API and writes them to Secrets Manager under `{stackPrefix}.{KEY}` (also stores the OIDC discovery-doc URL). `setSecret` is create-or-`putSecretValue` idempotent. Do not move this into the CFN template — CFN can't retrieve the app secret.
- **SES topic wiring is prod-only** — gated by `"prod".equalsIgnoreCase(stack)` (a raw string compare, not `Constants.isProd`). Match the existing check if you add similar prod-gating here.

## WaitConditionHandler (`waitconditions/`)

`WaitConditionHandler` implementations are a **second Guice `Multibinder` extension point** (parallel to `VelocityContextProvider`), bound in `TemplateGuiceModule`.

- **Each handler is matched to a CFN template resource by exact string** via `getWaitConditionId()` — it must equal the wait-condition logical id in the `.vpt` template, or the handler never fires.
- **Handlers must be idempotent** — they are invoked repeatedly while the outer stack poll runs (e.g. `SynapseHelpKnowledgeBaseDataSourceSync` returns early if an ingestion job already exists).
- **`SynapseHelpKnowledgeBaseDataSourceSync.handle()` runs its own blocking `Thread.sleep(10s)` poll loop** *inside* the outer `CloudFormationClientWrapperImpl.waitForStackToComplete` loop. This nested blocking counts against the outer wait timeout — keep handler work bounded.
