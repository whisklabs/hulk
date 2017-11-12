package com.whisk.hulk

sealed abstract class RowDecodingFailure(val message: String) extends Exception {

  final override def getMessage: String = message

  override final def toString: String = s"RowDecodingFailure($message)"
}

case class ColumnNameNotFound(name: String)
    extends RowDecodingFailure("column doesn't exist. name=" + name)

case class ColumnIndexNotFound(index: Int)
    extends RowDecodingFailure("column index out of bound. index=" + index)

case class RowValueDecodingFailure(msg: String, cause: Option[Throwable])
    extends RowDecodingFailure(msg) {

  override def getCause: Throwable = cause.orNull
}
