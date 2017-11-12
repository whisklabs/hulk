package com.whisk.hulk.cockroach

import com.github.mauricio.async.db.column.{ColumnDecoder, LongEncoderDecoder}
import com.github.mauricio.async.db.postgresql.column.{ColumnTypes, PostgreSQLColumnDecoderRegistry}

import scala.annotation.switch

object CockroachColumnDecoderRegistry {

  val Instance = new CockroachColumnDecoderRegistry
}

class CockroachColumnDecoderRegistry extends PostgreSQLColumnDecoderRegistry {

  override def decoderFor(kind: Int): ColumnDecoder = {
    (kind: @switch) match {
      case ColumnTypes.Integer =>
        LongEncoderDecoder
      case k => super.decoderFor(kind)
    }
  }
}
