# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| v1.6   | :white_check_mark: |
| Old version  | :x:                |

I recommend always running the latest version of Media-Multitool.

## Reporting a Vulnerability

If you discover a security vulnerability in Media-Multitool, **please do not open a public issue.**

Instead, report it privately via [GitHub Security Advisories](https://github.com/Jussi699/media-multitool/security/advisories/new).

Please include:

- A description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)


I will acknowledge your report within 48 hours and aim to release a fix as quickly as possible. You will be credited in the release notes unless you prefer otherwise.

## Scope

This policy covers the Media-Multitool desktop application and its source code. It does not cover third-party dependencies — please report those to the respective maintainers.

## Security Design

Media Multitool processes your media files locally on your computer. I take your privacy seriously:

- **No telemetry:** The application code contains no analytics or telemetry collection.
- **Local processing:** Image, audio, video, and PDF operations run on your device; files are not uploaded to a processing service.
- **No automatic network requests:** The application does not make network requests for processing or update checks. Links such as GitHub and GPS map links open externally only when you choose to visit them.
- **Open source:** The project is available under the MIT License, so its code can be inspected and audited.

