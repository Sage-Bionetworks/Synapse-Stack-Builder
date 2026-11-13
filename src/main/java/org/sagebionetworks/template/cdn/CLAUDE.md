# CLAUDE.md — `cdn` (incl. `webacl`)

Builds CloudFront distributions. `CdnBuilderImpl` handles three distribution types (`Type.PORTAL`, `Type.DATA`, `Type.STATIC_CONTENT`); `webacl/CdnWebAclBuilderImpl` is a simpler template-only builder with no notable gotchas.

## Conventions

- **The CDN reuses the portal Beanstalk's ACM certificate.** `createContext` reads `PROPERTY_KEY_BEANSTALK_SSL_ARN + "portal"` for the CDN's ACM ARN (comment: "The ACM ARN is the same as the one used for portal"). Non-obvious cross-resource reuse — don't create a separate cert.
- **`Type` (PORTAL/DATA/STATIC_CONTENT) drives duplicated if/else branches** in both `createContext` and `buildCdnStack`, each with a matching `IllegalArgumentException` guard. If another `Type` is added, update both branch sites in sync.
- **The STATIC_CONTENT CDN serves public, unsigned content** (no key group) from the `${stack}.static-content.sagebase.org` bucket at `static.<dev|prod>.sagebase.org`. Content is grouped in top-level folders by use (e.g. `email-images/`); the CDN has no origin path, so a folder is part of the URL. It reuses the DATA CDN's certificate property, so that certificate must also cover the `static.*` names. The bucket's read grant lives in `s3/s3-bucket-policy.json.vpt` and imports this stack's exported ARN, so on a first deployment the bucket policy stack can only succeed after this stack exists.
- **The DATA CDN hashes its public key with MD5** (`DigestUtils.md5Hex`) for a template context variable — this is a cache-key/identifier use, not security; leave it as MD5 to match the template.
