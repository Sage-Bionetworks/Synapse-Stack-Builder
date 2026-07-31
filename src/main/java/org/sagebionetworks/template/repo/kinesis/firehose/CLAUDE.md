# CLAUDE.md — `repo/kinesis/firehose`

Builds Kinesis Firehose delivery streams that land records in S3 (optionally converting JSON → Parquet via a Glue table). `KinesisFirehoseVelocityContextProvider` post-processes the configured streams before rendering.

## Conventions

- **`devOnly` streams are dropped only on the prod stack.** `addToContext` filters out `stream.isDevOnly()` streams when the stack equals `Constants.PROD_STACK_NAME`; on all other stacks they deploy. Preserve this filter when editing stream selection.
- **Glue database name is derived, not configured.** It is `stack + instance + GLUE_DB_SUFFIX` where `GLUE_DB_SUFFIX = "firehoseLogs"`. This constant is imported cross-package by `repo/athena` as its default query database — changing it affects Athena queries too.
- **Stream bucket and table names are parameterized** — bucket via `TemplateUtils.replaceStackVariable(...)`, table name via `stack + instance + name`. New streams should follow this, not hardcode names.

## Validation (`KinesisFirehoseConfigValidator`)

- `bufferFlushInterval` and `bufferFlushSize` are clamped to hardcoded MIN/MAX constants on `KinesisFirehoseStreamDescriptor`; out-of-range values throw `IllegalStateException`.
- **PARQUET format requires a non-null `tableDescriptor` with ≥1 column**; JSON does not. Adding a Parquet stream without a table definition fails validation.
