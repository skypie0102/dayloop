# Candidate signing identity

`candidate-test-key.p12` is an intentionally public, non-production signing key
for the `com.shadowmonarchbooks.dayloop.candidate` test channel. It lets candidate
APKs built on different CI runners update one another without deleting progress.
The candidate build remains debuggable and uses a separate application ID.

| Setting | Value |
|---|---|
| Store type | PKCS12 |
| Alias | `dayloop-candidate` |
| Store/key password | `android` |
| Certificate SHA-256 | `a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334` |
| Certificate validity | January 1, 2026–January 1, 2056 |

This public identity offers no production publisher authentication. Never use it
for the production application ID, production releases or store uploads. The
private production signing configuration remains separate.

CI, native review and the candidate workflow run `verify-candidate-signing.sh`
against the built APK. Android SDK `apksigner` verifies the APK signature, then
the script checks its certificate against the pinned fingerprint above. Keeping
the existing key preserves the candidate update identity.

Candidates through rc6 used a fresh runner-generated debug key. Their signatures
differ from rc7 and cannot be upgraded in place. Export progress before removing
an older candidate, install rc7, import the backup and select each restored
profile as described in [Progress backups](../../docs/progress-backups.md).
