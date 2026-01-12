# Repository Guidelines

## Project Structure & Module Organization
- Root build/config: `bleep.yaml` (project definitions), `bleep.publish.yaml` (release metadata), `.scalafmt.conf` (formatting rules).
- Module `tcn/`: core ZIO-based Postgres triggered change notification service.
  - Sources: `tcn/src/scala/io/github/nafg/tcn/`.
- Module `tcn-slick/`: Slick integration utilities.
  - Sources: `tcn-slick/src/scala/io/github/nafg/tcn/slick/`.
- No test directories are present in the repository at the moment.

## Build, Test, and Development Commands
- `bleep compile`: compile all projects (this is what CI runs).
- `bleep compile tcn` / `bleep compile tcn-slick`: compile a single module when iterating locally.
- `bleep publish -- --mode=portal-api:AUTOMATIC`: release via the configured publish script (used in CI on version tags).

## Coding Style & Naming Conventions
- Formatting is enforced by Scalafmt 3.10.3 with `maxColumn = 120` and IntelliJ preset; run your formatter before committing.
- Keep package names under `io.github.nafg.tcn` and follow standard Scala naming: `CamelCase` for types, `camelCase` for vals/defs.
- Prefer small, focused files; follow existing layout when adding new services or integrations.

## Testing Guidelines
- There are no tests or test framework dependencies configured currently.
- If you add tests, place them under `tcn/src/test/scala` or `tcn-slick/src/test/scala` and ensure they run via bleep’s test task (e.g., `bleep test`).
- Keep test names descriptive and aligned with the public API (e.g., `TriggeredChangeNotificationServiceSpec`).

## Commit & Pull Request Guidelines
- Commit messages in history are short, imperative, and sometimes prefixed with a scope (e.g., `ci: bump ...`, `Update dependency ...`). Follow that style.
- PRs should include a clear summary, any relevant issue links, and the exact bleep commands run.
- If behavior changes, include a brief rationale and any migration notes in the PR description.

## Release Notes & Versioning
- CI publishes only on tags that start with `v` (e.g., `v0.1.0`).
- Ensure the release description aligns with `bleep.publish.yaml` metadata.
