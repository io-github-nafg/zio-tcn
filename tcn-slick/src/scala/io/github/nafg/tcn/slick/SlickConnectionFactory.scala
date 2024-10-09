package io.github.nafg.tcn.slick

import java.sql.Connection

import slick.jdbc.JdbcBackend.Database
import io.github.nafg.tcn.ConnectionFactory

import zio.{Scope, ZIO}

class SlickConnectionFactory(database: Database) extends ConnectionFactory {
  override def newConnection: ZIO[Scope, Throwable, Connection] =
    ZIO.fromAutoCloseable(ZIO.attemptBlocking(database.createSession().conn))
}
