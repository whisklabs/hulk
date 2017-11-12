package com.whisk.hulk

/**
  * Typeclass responsible for encoding a parameter of type T for sending to postgres
  * @tparam T The type which it encodes
  */
trait ValueEncoder[T] {
  def encode(t: T): Any
}

object ValueEncoder {

  def apply[T: ValueEncoder] = implicitly[ValueEncoder[T]]

  def instance[T](f: T => Any): ValueEncoder[T] = new ValueEncoder[T] {
    override def encode(t: T): Any = f(t)
  }

  def identityInst[T]: ValueEncoder[T] = instance(identity)

  implicit val string: ValueEncoder[String] = identityInst
  implicit val int: ValueEncoder[Int] = identityInst
  implicit val Long: ValueEncoder[Long] = identityInst
  implicit val float: ValueEncoder[Float] = identityInst
  implicit val double: ValueEncoder[Double] = identityInst
  implicit val boolean: ValueEncoder[Boolean] = identityInst

  @inline final implicit def option[T](implicit encodeT: ValueEncoder[T]): ValueEncoder[Option[T]] =
    new ValueEncoder[Option[T]] {
      override def encode(t: Option[T]): Any = t match {
        case None        => null
        case Some(value) => encodeT.encode(value)
      }
    }

  @inline final implicit def some[T](implicit encodeT: ValueEncoder[T]): ValueEncoder[Some[T]] =
    instance((v: Some[T]) => encodeT.encode(v.get))

  implicit object none extends ValueEncoder[None.type] {
    override def encode(t: None.type): Any = null
  }
}
