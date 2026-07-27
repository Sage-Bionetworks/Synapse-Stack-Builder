# CLAUDE.md — `cdn` (incl. `webacl`)

Builds CloudFront distributions. `CdnBuilderImpl` handles two distribution types (`Type.PORTAL`, `Type.DATA`); `webacl/CdnWebAclBuilderImpl` is a simpler template-only builder with no notable gotchas.

## Conventions

- **The CDN reuses the portal Beanstalk's ACM certificate.** `createContext` reads `PROPERTY_KEY_BEANSTALK_SSL_ARN + "portal"` for the CDN's ACM ARN (comment: "The ACM ARN is the same as the one used for portal"). Non-obvious cross-resource reuse — don't create a separate cert.
- **`Type` (PORTAL/DATA) drives duplicated if/else branches** in both `createContext` and `buildCdnStack`, each with a matching `IllegalArgumentException` guard. If a third `Type` is added, update both branch sites in sync.
- **The DATA CDN hashes its public key with MD5** (`DigestUtils.md5Hex`) for a template context variable — this is a cache-key/identifier use, not security; leave it as MD5 to match the template.
