# CLAUDE.md — `datawarehouse` (incl. `backfill`)

Deploys the Glue/Athena data-warehouse ETL infrastructure. `DataWarehouseBuilderImpl` builds the stack; `backfill/BackfillDataWarehouseBuilderImpl` runs one-off Glue backfill jobs over historical S3 partitions.

## Conventions

- **`copyArtifactFromGithub` downloads a GitHub release zip and re-uploads matched entries to S3.** It wraps the shared `ZipInputStream` with `ReleasableInputStream.wrap(...).disableClose()` so the S3 SDK does not close the stream mid-loop. Preserve the `disableClose()` — without it the upload closes the zip stream and subsequent entries fail.
- **The GitHub download pattern and the hardcoded AWS Glue Studio transform URLs** (`gs_explode.py`, `gs_common.py` under bucket `aws-glue-studio-transforms-510798373988-prod-us-east-1`) are **duplicated** in both `DataWarehouseBuilderImpl` and `backfill/BackfillDataWarehouseBuilderImpl`. Keep the two copies in sync if either changes.

## backfill/ constraints

- **`githubRepo` / `version` are hardcoded** (`"Synapse-ETL-Jobs"`, `"1.39.0"`) in `BackfillDataWarehouseBuilderImpl`, whereas the parent `DataWarehouseBuilderImpl` reads both from `DataWarehouseConfig`. This divergence is intentional (the backfill pins a specific job version) — do NOT "deduplicate" them into shared config.
- **Athena query completion is polled with an unbounded `Thread.sleep(5000)` loop** (no timeout/retry cap), plus a `Thread.sleep(2000)` throttle after each Glue job start. If you touch this, be aware there is no upper bound.
- **Glue partition params are reconstructed by a recursive S3-prefix walk** using fragile `indexOf`/substring parsing tied to hardcoded folder names. Changing the S3 layout breaks the parsing.
