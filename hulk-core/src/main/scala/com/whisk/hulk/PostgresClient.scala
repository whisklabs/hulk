package com.whisk.hulk

import com.github.mauricio.async.db
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.whisk.hulk.cockroach.CockroachConnectionFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait PostgresClient {

  def query(sql: String): Future[QueryResponse]

  def fetch(sql: String): Future[ResultSet]

  def executeUpdate(sql: String): Future[OK]

  /*
   * Run a single SELECT query and wrap the results with the provided function.
   */
  def select[T](sql: String)(f: Row => T): Future[Seq[T]]

  /*
   * Issue a single, prepared SELECT query and wrap the response rows with the provided function.
   */
  def prepareAndQuery[T](sql: String, params: Param[_]*)(f: Row => T): Future[Seq[T]]

  /*
   * Issue a single, prepared arbitrary query without an expected result set, and provide the affected row count
   */
  def prepareAndExecute(sql: String, params: Param[_]*): Future[Long]

  def isConnected: Boolean

  def close(): Future[Unit]
}

private class PostgresClientImpl(connection: db.Connection) extends PostgresClient {

  private def mapResultSet(rs: db.ResultSet): ResultSet = {
    val indexMap: Map[String, Int] = rs.columnNames.zipWithIndex.toMap
    ResultSet(rs.map(rowData => RowImpl(indexMap, rowData)))
  }

  private def toResultSet(result: db.QueryResult): ResultSet = {
    result.rows match {
      case Some(resultSet) => mapResultSet(resultSet)
      case None            => throw new Exception("not a select statement result")
    }
  }

  def query(sql: String): Future[QueryResponse] = {
    connection
      .sendQuery(sql)
      .map(r => QueryResponse(r.rowsAffected, resultSet = r.rows.map(mapResultSet)))
  }

  def fetch(sql: String): Future[ResultSet] = {
    connection.sendQuery(sql).map(toResultSet)
  }

  def executeUpdate(sql: String): Future[OK] = {
    connection.sendQuery(sql).map(r => OK(r.rowsAffected))
  }

  /*
   * Run a single SELECT query and wrap the results with the provided function.
   */
  def select[T](sql: String)(f: Row => T): Future[Seq[T]] = {
    fetch(sql).map(_.rows.map(f))
  }

  /*
   * Issue a single, prepared SELECT query and wrap the response rows with the provided function.
   */
  def prepareAndQuery[T](sql: String, params: Param[_]*)(f: Row => T): Future[Seq[T]] = {
    connection
      .sendPreparedStatement(sql, params.map(_.encoded))
      .map(result => toResultSet(result).rows.map(f))
  }

  /*
   * Issue a single, prepared arbitrary query without an expected result set, and provide the affected row count
   */
  def prepareAndExecute(sql: String, params: Param[_]*): Future[Long] = {
    connection.sendPreparedStatement(sql, params.map(_.encoded)).map(_.rowsAffected)
  }

  def isConnected: Boolean = connection.isConnected

  def close(): Future[Unit] = connection.disconnect.map(_ => Unit)
}

object PostgresClient {

  def from(connection: db.Connection): PostgresClient = new PostgresClientImpl(connection)

  def pooled(conf: db.Configuration, connectTimeout: Duration = 5.seconds): PostgresClient = {
    val factory = new CockroachConnectionFactory(conf)
    val pool = new ConnectionPool(factory, PoolConfiguration.Default)
    from(Await.result(pool.connect, connectTimeout))
  }
}
