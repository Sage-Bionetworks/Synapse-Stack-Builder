# CLAUDE.md — `repo/agent`

Contributes the Bedrock Agent CloudFormation resources to the Velocity context (`BedrockAgentContextProvider`, `BedrockGridAgentContextProvider`). Unlike other providers, the CFN is NOT authored in a `.vpt` template — it is loaded from a static JSON template (`bedrock_agent_template.json`) and mutated in Java via raw `org.json` `JSONObject`/`JSONArray` path-navigation, then spliced into the Velocity context.

## Constraints

- **Do NOT edit the shared JSON template's external portions directly.** `bedrock_agent_template.json` is shared with external people, so Sage-specific values are patched in Java (e.g. replacing knowledge-base ARNs/IDs with `Fn::ImportValue` references). The comment in `addToContext` marks this. Add substitutions in Java, not by editing the shared template.
- **Do NOT reformat the JSON string surgery.** The result is inserted via `context.put(..., "," + json.substring(1, json.length()-1))` — it strips the outer `{}` and prepends a comma so it merges into a surrounding object literal in the `.vpt`. Reformatting or reindenting this breaks the CFN merge.
- **Building the context has an S3 side effect.** `addToContext` uploads the OpenAPI action-group schema (`agent_open_api.json`) to `{stack}-configuration.sagebase.org` via `S3Client.putObject`. This I/O happens during context assembly, not deployment — non-obvious for a context provider.

The `JSONArray`/`JSONObject` index paths (e.g. `Policies[0]/PolicyDocument/Statement[n]`, `KnowledgeBases/Fn::If[1][0]`) are tightly coupled to the shared template's structure; if that template changes shape, these indices must be updated in lockstep.
