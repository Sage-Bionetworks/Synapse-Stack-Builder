# CLAUDE.md — `s3`

S3 buckets are configured **imperatively via the AWS SDK**, not declared in CloudFormation/Velocity like the rest of the codebase. `S3BucketBuilderImpl.buildAllBuckets()` reads `s3-buckets-config.json` and, for each bucket, does get-then-set idempotency: fetch current state, create/update only what differs (encryption, public-access-block, inventory, lifecycle rules, intelligent-tiering, notifications).

## Conventions

- **Adding a bucket is config-only** — add an entry to `s3/s3-buckets-config.json`; no Java changes needed for a plain bucket.
- **Reuse the generic `addOrUpdateRule(rules, bucket, ruleName, definition, ruleCreator, ruleUpdate)` helper** for lifecycle rules (retention, class-transition, abort-multipart) rather than writing bespoke rule diffing.
- **`createBucket` treats `BucketAlreadyOwnedByYou` (409) as non-fatal/idempotent** so a re-run continues past already-owned buckets (PLFM-9661). Do not "fix" this by re-throwing.

## Constraints

- **Do NOT convert the S3 → virus-scanner notification wiring to pure CloudFormation.** Bucket notifications for the scanner topic are set imperatively (`configureBucketNotification`) *after* the virus-scanner stack is built and its topic ARN is available, because CFN cannot express this without a circular dependency (comment cites cloudformation-coverage-roadmap issue #79). The imperative wiring across multiple buckets is a deliberate simpler alternative to the AWS custom-resource workaround.
