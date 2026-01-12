# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is `zio-tcn`, a ZIO-based library for listening to PostgreSQL triggered change notifications (TCN). The library provides a streaming interface for receiving database table change events via PostgreSQL's LISTEN/NOTIFY mechanism.

## Build System

This project uses **Bleep** (v0.0.14) as its build tool.

### Common Commands

- `bleep compile` - Compile all projects (what CI runs)
- `bleep compile tcn` - Compile the core module
- `bleep compile tcn-slick` - Compile the Slick integration module
- `bleep publish -- --mode=portal-api:AUTOMATIC` - Publish to Sonatype (used in CI on version tags)

### Build Configuration

- Main config: `bleep.yaml`
- Publish metadata: `bleep.publish.yaml`
- Uses `source-layout: cross-pure` (important for cross-building)
- Cross-compiles to Scala 2.13.15 and 3.3.4

## Architecture

### Module Structure

**tcn** (core module)
- Location: `tcn/src/scala/io/github/nafg/tcn/`
- Dependencies: ZIO Parser, PostgreSQL JDBC driver
- Core components:
  - `TriggeredChangeNotificationService` - Main service providing change notification streams
  - `TriggeredChangeNotification` - Data model representing a table change event
  - `ConnectionFactory` - Abstraction for database connection management

**tcn-slick** (integration module)
- Location: `tcn-slick/src/scala/io/github/nafg/tcn/slick/`
- Dependencies: Slick, depends on `tcn`
- Provides: `SlickConnectionFactory` - Slick-based implementation of `ConnectionFactory`

### Key Architectural Patterns

**Notification Flow**:
1. Service creates a PostgreSQL connection and executes `LISTEN tcn`
2. Polls for notifications using `PGConnection.getNotifications(1000)`
3. Parses notification payloads using ZIO Parser
4. Publishes parsed events to a ZIO Hub (1024 element sliding buffer)
5. Consumers subscribe via `stream(tableName, keyColumnName)` for filtered streams

**Notification Format**: Notifications are expected in format `"tableName",OPERATION,"key1"='value1',"key2"='value2'` where OPERATION is I (Insert), U (Update), or D (Delete).

**Parser Implementation**: Uses ZIO Parser with custom quoted string handling (double quotes for identifiers, single quotes for values, with escape support via quote doubling).

**Connection Management**: Uses ZIO's resource-safe `ZIO.fromAutoCloseable` pattern with proper scoping.

**Error Handling**:
- Parse errors are logged and the notification is dropped
- Connection failures trigger automatic reconnection via stream timeout/retry
- 10-minute timeout causes stream restart to handle stale connections

## Code Style

- Scalafmt 3.10.3 with `maxColumn = 120` and IntelliJ preset
- Dialect: `scala213source3` (Scala 2.13 with source 3 compatibility)
- Run formatter before committing
- Scala 2 code uses `-Xsource:3` for forward compatibility
- Strict Scala compiler options enabled

## Testing

Currently no tests are present. If adding tests:
- Place under `tcn/src/test/scala/` or `tcn-slick/src/test/scala/`
- Use descriptive names aligned with public API

## Git Workflow

- Main branch: `main`
- Commit style: Short, imperative messages, sometimes with scope prefix (e.g., `ci: bump ...`)
- CI runs on all commits to `main` and PRs
- Publishing only happens on tags matching `v*` (e.g., `v0.1.0`)
- Use `git switch` instead of `git checkout`

## Dependencies

- Core: ZIO Parser 0.1.11, PostgreSQL 42.7.8
- Slick integration: Slick 3.6.1
- Cross-builds for both Scala 2.13.15 and 3.3.4
- JVM: GraalVM Java 17 (22.3.1)