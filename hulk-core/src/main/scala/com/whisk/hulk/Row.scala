package com.whisk.hulk

import scala.util.{Failure, Try}

trait Row {
  def getOption[T](name: String)(implicit decoder: ValueDecoder[T]): Option[T]
  def getOption[T](index: Int)(implicit decoder: ValueDecoder[T]): Option[T]
  def get[T](name: String)(implicit decoder: ValueDecoder[T]): T
  def get[T](index: Int)(implicit decoder: ValueDecoder[T]): T
  def getTry[T](name: String)(implicit decoder: ValueDecoder[T]): Try[T]
  def getTry[T](index: Int)(implicit decoder: ValueDecoder[T]): Try[T]
  def getOrElse[T](name: String, default: => T)(implicit decoder: ValueDecoder[T]): T
  def getOrElse[T](index: Int, default: => T)(implicit decoder: ValueDecoder[T]): T
  def getAnyOption(name: String): Option[Any]
  def getAnyOption(index: Int): Option[Any]
}

private case class RowImpl(indexMap: Map[String, Int], columns: IndexedSeq[Any]) extends Row {

  private def requireColumn(name: String): Any = {
    columns(indexMap.getOrElse(name, throw ColumnNameNotFound(name)))
  }

  private def requireColumn(index: Int): Any = {
    if (index < columns.length) {
      columns(index)
    } else {
      throw ColumnIndexNotFound(index)
    }
  }

  private def column(index: Int): Option[Any] = {
    if (index < columns.length) {
      Some(columns(index))
    } else {
      None
    }
  }

  final def getOption[T](name: String)(implicit decoder: ValueDecoder[T]): Option[T] =
    Option(requireColumn(name)).flatMap(any => decoder.decode(any).toOption)

  final def getOption[T](index: Int)(implicit decoder: ValueDecoder[T]): Option[T] =
    Option(requireColumn(index)).flatMap(any => decoder.decode(any).toOption)

  final def get[T](name: String)(implicit decoder: ValueDecoder[T]): T = {
    decoder
      .decode(requireColumn(name))
      .fold({ t =>
        throw RowValueDecodingFailure("failure while decoding column. name=" + name, Some(t))
      }, identity[T])
  }

  final def get[T](index: Int)(implicit decoder: ValueDecoder[T]): T = {
    decoder
      .decode(requireColumn(index))
      .fold({ t =>
        throw RowValueDecodingFailure("failure while decoding column. index=" + index, Some(t))
      }, identity[T])
  }

  final def getTry[T](name: String)(implicit decoder: ValueDecoder[T]): Try[T] = {
    Try(requireColumn(name)).flatMap {
      case null =>
        Failure(RowValueDecodingFailure("encountered null value for column. name=" + name, None))
      case any => decoder.decode(any)
    }
  }

  final def getTry[T](index: Int)(implicit decoder: ValueDecoder[T]): Try[T] = {
    Try(requireColumn(index)).flatMap {
      case null =>
        Failure(RowValueDecodingFailure("encountered null value for column. index=" + index, None))
      case any => decoder.decode(any)
    }
  }

  final def getOrElse[T](index: Int, default: => T)(implicit decoder: ValueDecoder[T]): T =
    getTry[T](index).getOrElse(default)

  final def getOrElse[T](name: String, default: => T)(implicit decoder: ValueDecoder[T]): T =
    getTry[T](name).getOrElse(default)

  final def getAnyOption(index: Int): Option[Any] =
    Option(requireColumn(index))

  final def getAnyOption(name: String): Option[Any] =
    Option(requireColumn(name))
}
