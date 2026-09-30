# CLAUDE.md — `repo`

Orchestrates the repository environment stacks. `RepositoryTemplateBuilderImpl` is the central builder: it creates shared resources (RDS, secrets), then builds the per-environment stacks (repo services, workers, tables, migration). Sub-packages own individual resource types (`beanstalk/`, `ecs/`, `athena/`, `kinesis/`, `queues/`, `cloudwatchlogs/`, `appconfig/`, `glue/`, `agent/`, `grid/`).

## Deployment target branch

`buildEnvironments()` reads `PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS` (`DeploymentTarget` enum) and branches into `buildBeanstalkEnvironments()` or `buildEcsEnvironments()`. The two paths differ in build AND wait behavior — Beanstalk builds/waits per stack in the loop; ECS submits all stacks then waits. Do not "unify" them without preserving each path's ordering. See `ecs/CLAUDE.md` for the Fargate image build.

## Constraints

- **NOSNAPSHOT invariant** (`createDatabaseDescriptors`): the repo DB and the table DBs must agree on whether they use RDS snapshots. If one uses the `NOSNAPSHOT` sentinel and the other doesn't, it throws `IllegalStateException`. When editing snapshot-identifier properties, keep repo and tables consistent.
- **The DB subnet group is pruned, and the databases are re-checked after deploy** (PLFM-9965). `createSharedContext()` puts `DATABASE_SUBNETS` — the color's private subnets narrowed to AZs offering *every* descriptor's `DBInstanceClass` (`getDatabaseSubnets` → `VpcSubnetUtils` → `RdsClientWrapper`) — and `main-repo-shared-resources-template.json.vpt` renders `SubnetIds` from it instead of importing the subnet export whole. `VpcSubnetUtils.getDatabaseSubnets` is shared with `IdGeneratorBuilderImpl` and returns the subnet ids **already quoted and comma separated**, so both templates substitute it whole (`"SubnetIds": [${databaseSubnets}]`) rather than looping — keep the quoting in Java if you change either side. `buildAndDeploy()` then calls `validateDatabases()` after `waitForStackToComplete`, because a completed stack does not mean the instances match the template. `IdGeneratorBuilderImpl` does the same for its single database. Keep the shared subnet group and change its `SubnetIds` in place — that is a non-disruptive `ModifyDBSubnetGroup`, whereas introducing a *new* subnet group would migrate the existing DB instance.
- **`Constants.RDS_ENGINE` / `RDS_ENGINE_VERSION` deliberately duplicate the templates.** They feed `DescribeOrderableDBInstanceOptions`; the templates keep `Engine`/`EngineVersion` hardcoded because `Engine` is an immutable CFN property and re-rendering it would replace the database. `RepositoryTemplateBuilderImplTest.validateEngineMatchesConstants` and `IdGeneratorBuilderImplTest.testBuildEngineMatchesConstants` are the drift guards — update both sides on an engine upgrade.
- **`TargetGroup` naming is a cross-site contract.** `new TargetGroup(type, stack, instance, number)` produces a name that **must match** the convention used by `dns-record-to-stack-mapping` and by `beanstalk/ssl/ElasticBeanstalkExtentionBuilderImpl`. Changing the format requires updating every construction site (comment marks this at the ALB target-group export).
- **CFN outputs are parsed by string-splitting** — when consuming a stack output, match the existing brittle parsing rather than assuming structured access.

## Related

- Snapshot/secret/CMK naming (`alias/synapse/<stack>/<instance>/cmk`, master secret `<stack>.<key>`, secrets S3 key `Stack/<stack>-<instance>-secrets.properties`) is shared by `beanstalk/SecretBuilderImpl` and `IdGeneratorBuilderImpl` — see `beanstalk/CLAUDE.md`.
