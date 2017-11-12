package com.whisk.hulk.tests

import java.sql.Timestamp
import java.time.Instant

import com.whisk.hulk.{ColumnNameNotFound, OK}
import com.whisk.hulk.testing.PostgresTestkit
import org.scalatest.{FunSuite, MustMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

class IntegrationSpec extends FunSuite with PostgresTestkit with ScalaFutures with MustMatchers {

  private implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  private lazy val client = pgClient.get()

  val pgTestTable = "test_table"

  def cleanDb() = {
    val dropResult = client.execute("DROP TABLE IF EXISTS %s".format(pgTestTable)).futureValue

    val createTableResult = client.execute("""
        |CREATE TABLE %s (
        | str_field VARCHAR(40),
        | int_field INT,
        | double_field DOUBLE PRECISION,
        | timestamp_field TIMESTAMP WITH TIME ZONE,
        | bool_field BOOLEAN
        |)
      """.stripMargin.format(pgTestTable)).futureValue
  }

  def insertSampleData(): Unit = {
    val insertDataQuery =
      client.execute("""
        |INSERT INTO %s VALUES
        | ('hello', 1234, 10.5, '2015-01-08 11:55:12-0800', TRUE),
        | ('hello', 5557, -4.51, '2015-01-08 12:55:12-0800', TRUE),
        | ('hello', 7787, -42.51, '2013-12-24 07:01:00-0800', FALSE),
        | ('hello', null, null, '2015-02-24 07:01:00-0800', null),
        | ('goodbye', 4567, 15.8, '2015-01-09 16:55:12+0500', FALSE)
      """.stripMargin.format(pgTestTable))

    val response = insertDataQuery.futureValue

    response mustEqual OK(5)
  }

  test("insert and select rows") {
    cleanDb()
    insertSampleData()

    val resultRows = client
      .select(
        "SELECT * FROM %s WHERE str_field='hello' ORDER BY timestamp_field".format(pgTestTable)
      )(identity)
      .futureValue

    resultRows.size mustEqual 4

    val firstRow = resultRows.head

    firstRow.getOption[String]("str_field") must equal(Some("hello"))
    firstRow.getOption[Int]("int_field") must equal(Some(7787))
    firstRow.getOption[Double]("double_field") must equal(Some(-42.51))
    firstRow.get[Instant]("timestamp_field") must equal(
      new Timestamp(1387897260000L).toInstant
    )
    firstRow.getOption[Boolean]("bool_field") must equal(Some(false))
    intercept[ColumnNameNotFound](firstRow.getOption[String]("bad_column"))

    // handle nullable values
    val lastRow = resultRows(3)
    lastRow.getOption[Int]("int_field") mustBe 'empty
    lastRow.getOption[Double]("double_field") mustBe 'empty
    lastRow.getOption[Boolean]("bool_field") mustBe 'empty
    assert(lastRow.getTry[Int]("int_field").isFailure)
  }
}
