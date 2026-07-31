# CLAUDE.md — `template` (top-level)

Shared infrastructure used by every builder: the Guice module, the CloudFormation client wrapper, AWS client wrappers, and `Constants`. Sub-packages own specific resource types.

## `CloudFormationClientWrapperImpl` — do not "clean up" these

- **Templates are never sent inline.** `createStack`/`updateStack` upload the template body to S3 first (`executeWithS3Template` → `saveTemplateToS3`, a `templates/<stack>-<uuid>.json` object) and delete it in a `finally` regardless of outcome. Preserve the upload-then-delete flow.
- **`waitForStackToComplete` has deliberate `switch` fallthrough** (no `break`): `CREATE_IN_PROGRESS`/`UPDATE_IN_PROGRESS` call `handleWaitConditions` then fall through into the sleep/log case; `UPDATE_ROLLBACK_COMPLETE` falls through to the `default` throw **unless** the stack started in that state (idempotency guard `startedInUpdateRollbackComplete`). Adding `break`s here changes behavior — don't.
- **"No updates are to be performed" is string-matched** and treated as non-fatal (`NO_UPDATES_ARE_TO_BE_PERFORMED`). Keep this — CFN signals a no-op update via exception.
- **`describeStack` swallows all `CloudFormationException`s as "stack does not exist"** (returns `Optional.empty()`), conflating a missing stack with an API error. This is existing, deliberate-for-now behavior (its own `// TODO: can this case happen?`). Flag rather than silently "fix".

## `TemplateGuiceModule` conventions

- **Two Multibinder extension points**, both bound here: `Multibinder<VelocityContextProvider>` (contributes template context vars — the main extension point the root CLAUDE.md documents) and `Multibinder<WaitConditionHandler>` (post-deploy CFN wait-condition handlers — see `global/CLAUDE.md`). A new provider/handler is registered by adding a binding here.
- **Region is hardcoded `Region.US_EAST_1`** on ~15 AWS client providers. New client providers should match.
- Cross-account "image-central" access uses a one-off STS `AssumeRoleRequest` / `StsAssumeRoleCredentialsProvider` provider — reuse this pattern if another cross-account client is ever needed.
