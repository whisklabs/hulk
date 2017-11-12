package com.whisk.hulk

import com.github.mauricio.async.db

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PostgresClient(connection: db.Connection) {

  private def mapResultSet(rs: db.ResultSet): ResultSet = {
    val indexMap: Map[String, Int] = rs.columnNames.zipWithIndex.toMap
    ResultSet(rs.map(rowData => RowImpl(indexMap, rowData)))
  }

  def query(sql: String): Future[QueryResponse] = {
    connection
      .sendQuery(sql)
      .map(r => QueryResponse(r.rowsAffected, resultSet = r.rows.map(mapResultSet)))
  }

  def fetch(sql: String): Future[ResultSet] = {
    connection.sendQuery(sql).map { result =>
      result.rows match {
        case Some(resultSet) => mapResultSet(resultSet)
        case None            => throw new Exception("not a select statement result")
      }
    }
  }

  def execute(sql: String): Future[OK] = {
    connection.sendQuery(sql).map(r => OK(r.rowsAffected))
  }

  /*
   * Run a single SELECT query and wrap the results with the provided function.
   */
  def select[T](sql: String)(f: Row => T): Future[Seq[T]] = {
    fetch(sql).map {
      case ResultSet(rows) =>
        rows.map(f)
    }
  }

  def isConnected: Boolean = connection.isConnected
}
