package io.github.nafg.tcn.slick

import java.sql.Connection

import slick.jdbc.JdbcBackend
import io.github.nafg.tcn.ConnectionFactory

import zio.{Scope, ZIO}

class SlickConnectionFactory(database: JdbcBackend#Database) extends ConnectionFactory {
  override def newConnection: ZIO[Scope, Throwable, Connection] =
    ZIO.fromAutoCloseable(ZIO.attemptBlocking(database.createSession().conn))
}
