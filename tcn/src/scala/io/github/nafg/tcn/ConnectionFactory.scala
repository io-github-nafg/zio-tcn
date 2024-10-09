package io.github.nafg.tcn

import java.sql.Connection

import zio.{Scope, ZIO}

trait ConnectionFactory {
  def newConnection: ZIO[Scope, Throwable, Connection]
}
