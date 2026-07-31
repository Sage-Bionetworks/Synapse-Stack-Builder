# CLAUDE.md — `nlb`

Builds Network Load Balancers and binds them to the repo services. Two builders with deliberately different behavior.

## Constraints

- **The two builders diverge on stack waiting — do NOT harmonize them.** `NetworkLoadBalancerBuilderImpl.buildAndDeploy()` does **not** call `waitForStackToComplete` (fire-and-forget); `BindNetworkLoadBalancerBuilderImpl.buildAndDeploy()` **does** wait. They share JSON-reformat/stack-name boilerplate but the wait difference is intentional — unifying them changes deployment behavior.
- **`NetworkLoadBalancer` reuses the cross-package static `IpAddressPoolBuilderImpl.ipAddressName(shortName, az)`** (in `ip/address`) to reconstruct Elastic-IP allocation names that must match the EIP pool stack. There is no compiler-visible link — if you change the `"%sAZ%d"` naming format in `ip/address`, NLB↔EIP association breaks.
- **`RecordToStackMapping.validateTarget` enforces a strict 4-part hyphen target format** (`{portal|repo}-{prod|dev}-###-#`) via `EnvironmentType`. Silent format changes elsewhere break this validation.
