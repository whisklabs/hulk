package com.whisk.hulk

case class QueryResponse(rowsAffected: Long, resultSet: Option[ResultSet])

case class ResultSet(rows: IndexedSeq[Row])

case class OK(rowsAffected: Long)
