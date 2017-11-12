package com.whisk.hulk.circe

import com.whisk.hulk.ValueDecoder
import io.circe.parser._
import io.circe.{Json, JsonObject}

trait CirceValueDecoders {

  final implicit val circeJsonDecoder: ValueDecoder[Json] =
    ValueDecoder.string.flatMap(jsonStr => parse(jsonStr).toTry)

  final implicit val circeJsonObjectDecoder: ValueDecoder[JsonObject] = {
    circeJsonDecoder.flatMap(_.as[JsonObject].toTry)
  }
}

object CirceValueDecoders extends CirceValueDecoders
