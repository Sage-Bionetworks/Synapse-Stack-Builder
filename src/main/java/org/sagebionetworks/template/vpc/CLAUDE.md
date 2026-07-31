# CLAUDE.md — `vpc`

VPC and subnet CloudFormation templates. Subnets are grouped by color (red/blue/green/orange) mapping to AZs; `SubnetTemplateBuilderImpl` builds public and per-color private subnet stacks.

## Constraints

- **`SubnetTemplateBuilderImpl.VPC_ENDPOINT_SERVICES` hardcodes the Bedrock VPC endpoints** (`bedrock, bedrock-agent, bedrock-runtime, bedrock-agent-runtime`), fed to every subnet group. When `repo/agent` or `repo/grid` starts depending on a new Bedrock (or other AWS) service reached over a VPC endpoint, add it to this list — otherwise those environments silently lose connectivity to the new service.
