# Security Policy

Copyright (c) 2026 ByteDance Ltd. and/or its affiliates.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).

## Reporting Security Issues

Do not open public issues for vulnerabilities or sensitive integration details. Report suspected
security issues to `src@bytedance.com` and include:

- Affected version or commit.
- Device and Android version.
- Steps to reproduce.
- Expected and observed behavior.
- Any logs with secrets, package names, user identifiers, and tokens removed.

## Demo Scope

This repository is a public SAEP client demo. It does not include credentials, signing keys, internal repositories, local binary SDKs, or private framework implementations.

## Handling Sensitive Data

Do not add credentials, tokens, signing material, private URLs, or user audit data to this repository. Audit and agent output rendered by the app is intentionally bounded and should be treated as diagnostic data.
