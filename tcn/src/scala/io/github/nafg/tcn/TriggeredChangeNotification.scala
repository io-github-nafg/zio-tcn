package io.github.nafg.tcn

import zio.parser.*

case class TriggeredChangeNotification(
  tableName: String,
  operation: TriggeredChangeNotification.Trigger,
  keyValuePairs: Map[String, String]
)
object TriggeredChangeNotification {
  sealed trait Trigger
  object Trigger {
    case object Insert extends Trigger
    case object Update extends Trigger
    case object Delete extends Trigger
  }

  /** Parses a quoted string.
    *
    * A single matching quote ends the string; if the quote is repeated it is treated as an escape and treated as a
    * single quote.
    */
  private def quotedParser(quote: Char): Parser[String, Char, String] = {
    val unescape =
      Parser.string(quote.toString + quote.toString, quote.toString)
    Parser.char(quote) ~>
      (Parser.charNotIn(quote).unit | unescape.unit).repeat.string <~
      Parser.char(quote)
  }

  private val doubleQuotedStringParser = quotedParser('"')
  private val singleQuotedStringParser = quotedParser('\'')
  private val keyValueParser           = (doubleQuotedStringParser <~ Parser.char('=')) ~ singleQuotedStringParser
  private val keyValuePairsParser      =
    keyValueParser.repeatWithSep(Parser.char(',')).map(_.toMap)
  private val operationParser          =
    Parser.char('I').as(TriggeredChangeNotification.Trigger.Insert) | Parser
      .char('U')
      .as(TriggeredChangeNotification.Trigger.Update) | Parser
      .char('D')
      .as(TriggeredChangeNotification.Trigger.Delete)

  val parser =
    (doubleQuotedStringParser <~ Parser.char(',')) ~
      (operationParser <~ Parser.char(',')) ~ keyValuePairsParser
}
