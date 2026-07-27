# CLAUDE.md — `repo/grid`

`GridContextProvider` contributes the grid websocket API Gateway resources to the Velocity context.

## Gotchas

- **`context.put("D", "$")`** exposes `$D` in the `.vpt` templates as a literal `$`. Use `$D` for dollar signs in the websocket request/response mapping JSON rather than fighting Velocity's `$`-variable escaping.
- **Fragments are loaded via `loadFileRemoveLineBreaks`** (`connect-request-template.vpt`, `default-request-template.vpt`, `disconnect-request-template.vpt`, `access-logs-setting-format.vpt`), which strips all line breaks (`\R+`). API Gateway mapping templates must be single-line, so keep these fragments free of anything that depends on line breaks.
- Grid depends on Bedrock VPC endpoints — see the VPC endpoint sync note in `src/main/java/org/sagebionetworks/template/vpc/CLAUDE.md` if you add a new Bedrock-backed service here.
