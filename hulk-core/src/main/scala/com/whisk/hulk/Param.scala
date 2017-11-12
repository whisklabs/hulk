package com.whisk.hulk

case class Param[T](value: T)(implicit val encoder: ValueEncoder[T]) {
  def encoded: Any = encoder.encode(value)
}

object Param {
  implicit def convert[T : ValueEncoder](t: T): Param[T] = Param(t)
}