# Publishing to Maven Central

Author: Viquar Khan

This project publishes via **GitHub Actions** (`.github/workflows/publish.yml`).
You do **not** need `~/.m2/settings.xml` on your laptop.

## Required GitHub Actions secrets

Repo → **Settings → Secrets and variables → Actions → New repository secret**.

| Secret name | What it is |
|-------------|------------|
| `CENTRAL_USERNAME` | User-token **username** from Central Portal |
| `CENTRAL_TOKEN` | User-token **password** from Central Portal |
| `GPG_PRIVATE_KEY` | ASCII-armored private key (`-----BEGIN PGP PRIVATE KEY BLOCK-----` …) |
| `GPG_PASSPHRASE` | Passphrase for that GPG key |

> The secret named `VJRSKA` is **not** used. Create the four names above.

### 1) Central Portal token

1. Sign in: https://central.sonatype.com  
2. Open: https://central.sonatype.com/usertoken  
3. **Generate User Token** — copy username + password once  
4. Add as `CENTRAL_USERNAME` and `CENTRAL_TOKEN`

### 2) GPG private key (from a machine that already has your signing key)

List keys:

```bash
gpg --list-secret-keys --keyid-format LONG
```

Export (replace `KEYID`):

```bash
gpg --armor --export-secret-keys KEYID
```

Paste the full block into secret `GPG_PRIVATE_KEY`.  
Put the key passphrase in `GPG_PASSPHRASE`.

Or with GitHub CLI (from that machine):

```bash
gpg --armor --export-secret-keys KEYID | gh secret set GPG_PRIVATE_KEY -R vaquarkhan/flink-mcp-enterprise-server
gh secret set GPG_PASSPHRASE -R vaquarkhan/flink-mcp-enterprise-server
gh secret set CENTRAL_USERNAME -R vaquarkhan/flink-mcp-enterprise-server
gh secret set CENTRAL_TOKEN -R vaquarkhan/flink-mcp-enterprise-server
```

(`gh secret set` without a pipe prompts for the value.)

## Run a publish

**Manual:** Actions → **publish** → **Run workflow**

**Or tag:**

```bash
git tag v0.3.1
git push origin v0.3.1
```

## Verify after success

https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/0.3.1/

Coordinates: `io.github.vaquarkhan:flink-mcp-server:0.3.1`

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>flink-mcp-server</artifactId>
  <version>0.3.1</version>
</dependency>
```

## Artifact links (after publish)

| | URL |
|---|-----|
| **Maven Central** | https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server |
| **repo1** | https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/ |
| **GitHub repo** | https://github.com/vaquarkhan/flink-mcp-enterprise-server |
