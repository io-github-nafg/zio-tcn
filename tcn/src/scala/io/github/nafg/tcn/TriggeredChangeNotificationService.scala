package io.github.nafg.tcn

import org.postgresql.PGConnection
import zio.stream.ZStream
import zio.{Hub, ZIO, ZLayer, durationInt}

trait TriggeredChangeNotificationService  {
  def stream(
    tableName: String,
    keyColumnName: String = "id"
  ): ZStream[Any, Nothing, (TriggeredChangeNotification.Trigger, String)]
}
object TriggeredChangeNotificationService {

  private def stream: ZStream[ConnectionFactory, Nothing, TriggeredChangeNotification] =
    ZStream
      .serviceWithStream[ConnectionFactory] { connectionFactory =>
        ZStream.scoped[Any](connectionFactory.newConnection)
      }
      .tap { conn =>
        ZIO.scoped[Any](
          ZIO
            .fromAutoCloseable(ZIO.attemptBlocking(conn.createStatement()))
            .tap(s => ZIO.attemptBlocking(s.execute("LISTEN tcn")))
        )
      }
      .map(_.unwrap(classOf[PGConnection]))
      .flatMap { conn =>
        ZStream.logInfo("Beginning to listen") *>
          ZStream
            .fromIterableZIO(ZIO.attemptBlocking(Option(conn.getNotifications(1000)).map(_.toSeq).getOrElse(Nil)))
            .forever
            .flatMap { notification =>
              TriggeredChangeNotification.parser.parseString(notification.getParameter) match {
                case Right((tableName, operation, keyValuePairs)) =>
                  ZStream.succeed(TriggeredChangeNotification(tableName, operation, keyValuePairs))
                case Left(error)                                  =>
                  ZStream.logError(s"Failed to parse notification:\n${error.pretty}") *> ZStream.empty
              }
            }
      }
      .catchAllCause(ZStream.logErrorCause(_) *> ZStream.empty)
      .forever
      .timeoutTo(10.minutes)(stream)

  val layer: ZLayer[ConnectionFactory, Nothing, TriggeredChangeNotificationService] =
    ZLayer {
      for {
        hub <- Hub.sliding[TriggeredChangeNotification](1024)
        _   <- stream.runForeach(hub.publish(_)).fork
      } yield new TriggeredChangeNotificationService {
        override def stream(tableName: String, keyColumnName: String) =
          ZStream
            .fromHub(hub)
            .collect { case TriggeredChangeNotification(`tableName`, trigger, keyValuePairs) =>
              keyValuePairs
                .get(keyColumnName)
                .map(trigger -> _)
            }
            .collectSome
      }
    }
}
