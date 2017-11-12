package com.whisk.hulk.circe

import com.whisk.hulk.Row
import io.circe.{Decoder, Json}

import scala.util.Try

trait CirceRowImplicits {

  implicit class CirceRichRow(row: Row) extends CirceRowOps {

    private def fromValue[T](value: Any)(implicit ev: Decoder[T]): Try[T] = {
      circeJsonDecoder
        .decode(value)
        .flatMap(_.as[T].toTry)
    }

    override def jsonOption[T](name: String)(implicit decoder: Decoder[T]): Option[T] =
      row.getOption[Json](name).flatMap(_.as[T].toOption)

    override def jsonOption[T](index: Int)(implicit decoder: Decoder[T]): Option[T] =
      row.getOption[Json](index).flatMap(_.as[T].toOption)

    override def json[T](name: String)(implicit decoder: Decoder[T]): T =
      row.get[Json](name).as[T].fold(t => throw t, identity)

    override def json[T](index: Int)(implicit decoder: Decoder[T]): T =
      row.get[Json](index).as[T].fold(t => throw t, identity)

    override def jsonOrElse[T](name: String, default: => T)(implicit decoder: Decoder[T]): T = {
      jsonOption[T](name).getOrElse(default)
    }

    override def jsonOrElse[T](index: Int, default: => T)(implicit decoder: Decoder[T]): T = {
      jsonOption[T](index).getOrElse(default)
    }

    override def jsonTry[T](name: String)(implicit decoder: Decoder[T]): Try[T] =
      row.getTry[Json](name).flatMap(_.as[T].toTry)

    override def jsonTry[T](index: Int)(implicit decoder: Decoder[T]): Try[T] =
      row.getTry[Json](index).flatMap(_.as[T].toTry)

  }
}

object CirceRowImplicits extends CirceRowImplicits
