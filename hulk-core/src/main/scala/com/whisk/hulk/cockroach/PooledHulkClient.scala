package com.whisk.hulk.cockroach

import com.whisk.hulk._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PooledHulkClient(pool: AsyncConnectionPool) extends HulkClient {

  private def wrap[T](f: HulkClient => Future[T]): Future[T] = {
    pool.take().flatMap { conn =>
      val future = f(new HulkClientImpl(conn))
      future.onComplete { _ =>
        pool.giveBack(conn)
      }
      future
    }
  }

  override def query(sql: String): Future[QueryResponse] = wrap(_.query(sql))

  override def fetch(sql: String): Future[ResultSet] = wrap(_.fetch(sql))

  override def executeUpdate(sql: String): Future[OK] = wrap(_.executeUpdate(sql))

  override def select[T](sql: String)(f: Row => T): Future[Seq[T]] = wrap(_.select(sql)(f))

  override def prepareAndQuery[T](sql: String, params: Param[_]*)(f: Row => T): Future[Seq[T]] =
    wrap(_.prepareAndQuery(sql, params: _*)(f))

  override def prepareAndExecute(sql: String, params: Param[_]*): Future[Long] =
    wrap(_.prepareAndExecute(sql, params: _*))

  override def close(): Unit = pool.close()
}
