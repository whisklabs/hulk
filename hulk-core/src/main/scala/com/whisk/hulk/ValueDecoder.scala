package com.whisk.hulk

import java.time.Instant

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait ValueDecoder[T] {

  def decode(any: Any): Try[T]
}

object ValueDecoder {

  def apply[T: ValueDecoder] = implicitly[ValueDecoder[T]]

  def instance[T](f: Any => Try[T]): ValueDecoder[T] =
    new ValueDecoder[T] {
      override def decode(any: Any): Try[T] = f(any)
    }

  def fromPF[T: ClassTag](pf: PartialFunction[Any, T]): ValueDecoder[T] = { any =>
    if (pf.isDefinedAt(any)) {
      Success(pf(any))
    } else {
      Failure(
        RowValueDecodingFailure(
          s"decoding exception while converting value '$any' (of type ${any.getClass}) into type ${implicitly[
            ClassTag[T]].runtimeClass}",
          None))
    }
  }

  def directType[T: ClassTag]: ValueDecoder[T] = { any =>
    val clz = implicitly[ClassTag[T]].runtimeClass
    if (clz == any.getClass) {
      Success(any.asInstanceOf[T])
    } else {
      Failure(RowValueDecodingFailure(
        s"decoding exception while converting value '$any' (of type ${any.getClass}) into type $clz",
        None))
    }
  }

  implicit val string: ValueDecoder[String] = directType[String]

  implicit val int: ValueDecoder[Int] = fromPF {
    case v: java.lang.Integer => v
    case v: Int               => v
  }

  implicit val long: ValueDecoder[Long] = directType[Long]

  implicit val float: ValueDecoder[Float] = fromPF {
    case f: Float  => f
  }

  implicit val double: ValueDecoder[Double] = fromPF {
    case f: Float  => f
    case f: Double => f
  }

  implicit val boolean: ValueDecoder[Boolean] = fromPF {
    case v: java.lang.Boolean => v
    case v: Boolean           => v
  }

  implicit val instant: ValueDecoder[Instant] = fromPF {
    case v: java.sql.Timestamp     => v.toInstant
    case v: org.joda.time.DateTime => Instant.ofEpochMilli(v.getMillis)
  }
}
