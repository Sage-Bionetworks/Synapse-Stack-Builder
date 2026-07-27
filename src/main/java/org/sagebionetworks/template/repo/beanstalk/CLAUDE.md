# CLAUDE.md — `repo/beanstalk` (incl. `beanstalk/ssl`)

Builds the Elastic Beanstalk deployment artifacts: downloads the app WAR from Artifactory, injects `.ebextensions`, uploads to S3, and generates the TLS material. `ssl/` is tightly coupled to this package (WAR bundling + cert generation) — treat them as one unit.

## Conventions

- **`ArtifactCopyImpl` is an idempotent cache.** `copyArtifactIfNeeded` guards on `s3Client.doesObjectExist(bucket, s3Key)` and only downloads from Artifactory + re-bundles the WAR when the S3 object is missing. Do not remove the existence check — it avoids re-downloading on every deploy.
- **`EnvironmentType` gates secrets.** Only `REPOSITORY_SERVICES` and `REPOSITORY_WORKERS` include secrets (`shouldIncludeSecrets()` / the `includeSecrets` flag); `PORTAL` does not. Code that wires secrets must respect this — don't assume every environment gets them.
- **`getShortName()` returns the `cnamePrefix`** (`repo`, `workers`, `portal`), not the enum name. Artifactory URL and S3 key use `pathName` (`services-repository`, etc.). These are distinct strings — use the right accessor.

## Constraints

- **Naming formats are load-bearing — do not change casually:**
  - `TargetGroup` name (`type-stack-instance-number`, dash-stripped for the short form) must match `dns-record-to-stack-mapping` and the ALB target-group export in `repo/RepositoryTemplateBuilderImpl` (two construction sites).
  - CMK alias `alias/synapse/<stack>/<instance>/cmk`, master secret key `<stack>.<key>`, and secrets S3 key `Stack/<stack>-<instance>-secrets.properties` are relied on by both `SecretBuilderImpl` and `repo/IdGeneratorBuilderImpl`.
- **`ElasticBeanstalkExtentionBuilderImpl.copyWarWithExtensions` has a strict ordered bundling pipeline** with ~9 hardcoded `.ebextensions` template paths. Do not reorder or rename the directory/file creation without checking every path constant stays consistent.
- **The X.509 cert is regenerated on every WAR build**, not cached (`CertificateBuilderImpl`: self-signed, RSA 2048, fixed DN, 1-year validity). Do not assume a stable cert across deploys.
