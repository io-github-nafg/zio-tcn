# zio-tcn

A ZIO-based library for listening to PostgreSQL
[triggered change notifications](https://www.postgresql.org/docs/current/tcn.html) (TCN).

## Overview

`zio-tcn` provides a streaming interface for receiving database table change events from PostgreSQL using the LISTEN/NOTIFY mechanism. When database triggers send notifications about INSERT, UPDATE, or DELETE operations, this library parses them and delivers them as type-safe ZIO streams.

## Features

- **ZIO Streams**: Change notifications delivered as `ZStream`
- **Filtered Subscriptions**: Subscribe to specific tables and key columns
- **Automatic Reconnection**: Handles connection failures with timeout-based retry
- **Type-Safe Parsing**: Uses ZIO Parser to parse notification payloads
- **Slick Integration**: Optional module for integration with Slick database library
- **Cross-Platform**: Supports both Scala 2.13 and Scala 3

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "io.github.nafg.tcn" %% "tcn" % "<version>"

// Optional: for Slick integration
libraryDependencies += "io.github.nafg.tcn" %% "tcn-slick" % "<version>"
```

Or for Bleep (`bleep.yaml`):

```yaml
projects:
  my-project:
    dependencies:
      - io.github.nafg.tcn::tcn:<version>
      # Optional: for Slick integration
      # - io.github.nafg.tcn::tcn-slick:<version>
```

## Usage

### Basic Setup

First, implement the `ConnectionFactory` trait to provide database connections:

```scala
import io.github.nafg.tcn.ConnectionFactory
import zio._
import java.sql.{Connection, DriverManager}

class SimpleConnectionFactory(url: String, user: String, password: String)
  extends ConnectionFactory {

  override def newConnection: ZIO[Scope, Throwable, Connection] =
    ZIO.fromAutoCloseable(
      ZIO.attemptBlocking(DriverManager.getConnection(url, user, password))
    )
}
```

### Using with Slick

If you're using Slick, use the provided `SlickConnectionFactory`:

```scala
import io.github.nafg.tcn.slick.SlickConnectionFactory
import slick.jdbc.PostgresProfile.api._

val database = Database.forConfig("mydb")
val connectionFactory = new SlickConnectionFactory(database)
```

### Subscribing to Changes

```scala
import io.github.nafg.tcn._
import zio._

val program = ZIO.scoped {
  for {
    // Create the service layer
    service <- ZIO.service[TriggeredChangeNotificationService]

    // Subscribe to changes on the "users" table, tracking by "id" column
    _ <- service
      .stream(tableName = "users", keyColumnName = "id")
      .foreach { case (trigger, id) =>
        trigger match {
          case TriggeredChangeNotification.Trigger.Insert =>
            Console.printLine(s"User $id was inserted")
          case TriggeredChangeNotification.Trigger.Update =>
            Console.printLine(s"User $id was updated")
          case TriggeredChangeNotification.Trigger.Delete =>
            Console.printLine(s"User $id was deleted")
        }
      }
  } yield ()
}

// Run with the service layer
val app = program.provide(
  ZLayer.succeed(connectionFactory),
  TriggeredChangeNotificationService.layer
)
```

### PostgreSQL Setup

Your PostgreSQL database needs to be configured with TCN triggers. The notification format expected by this library is:

```
"tableName",OPERATION,"key1"='value1',"key2"='value2',...
```

Where:
- `tableName` is the name of the table (double-quoted)
- `OPERATION` is one of: `I` (Insert), `U` (Update), `D` (Delete)
- Key-value pairs use double-quoted keys and single-quoted values

Example trigger setup:

```sql
-- Install the tcn extension
CREATE EXTENSION IF NOT EXISTS tcn;

-- Create a trigger on your table
CREATE TRIGGER users_tcn_trigger
  AFTER INSERT OR UPDATE OR DELETE ON users
  FOR EACH ROW EXECUTE FUNCTION triggered_change_notification();
```

Consult the [PostgreSQL TCN documentation](https://www.postgresql.org/docs/current/tcn.html) for complete setup instructions.

## How It Works

1. The service establishes a PostgreSQL connection and executes `LISTEN tcn`
2. It polls for notifications using `PGConnection.getNotifications()`
3. Notifications are parsed using ZIO Parser into structured data
4. Events are published to a ZIO Hub (sliding buffer of 1024 elements)
5. Consumers can subscribe to filtered streams for specific tables
6. Connection failures trigger automatic reconnection via 10-minute timeout

## Architecture

- **tcn**: Core module with ZIO-based notification service
- **tcn-slick**: Optional Slick integration providing `SlickConnectionFactory`

## Contributing

Contributions are welcome! Please ensure:
- Code is formatted with Scalafmt (run formatter before committing)
- Commit messages follow the existing style (short, imperative)
- PRs include a clear description of changes

## Building

This project uses [Bleep](https://github.com/oyvindberg/bleep) as its build tool:

```bash
# Compile all projects
bleep compile

# Compile specific module
bleep compile tcn
bleep compile tcn-slick
```

## License

Apache-2.0

## Links

- [GitHub Repository](https://github.com/io-github-nafg/zio-tcn)
- [PostgreSQL TCN Extension](https://www.postgresql.org/docs/current/tcn.html)
- [ZIO Documentation](https://zio.dev)