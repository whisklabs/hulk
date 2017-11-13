package com.whisk.hulk.tests

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import com.whisk.hulk.{ColumnNameNotFound, OK}
import com.whisk.hulk.testing.CockroachTestKit
import org.scalatest.{FunSuite, MustMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._
import scala.concurrent.Await

class IntegrationSpec extends FunSuite with CockroachTestKit with ScalaFutures with MustMatchers {

  private implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  private lazy val client = hulkClient.get()

  val testTable = "test.test_table"

  def cleanDb() = {
    Await.ready(client.query("CREATE DATABASE IF NOT EXISTS test"), 5.seconds)
    val dropResult = client.executeUpdate("DROP TABLE IF EXISTS %s".format(testTable)).futureValue

    val createTableResult = client.executeUpdate("""
        |CREATE TABLE %s (
        | id SERIAL PRIMARY KEY,
        | str_field VARCHAR(40),
        | int_field INT,
        | double_field DOUBLE PRECISION,
        | timestamp_field TIMESTAMP WITH TIME ZONE,
        | bool_field BOOLEAN
        |)
      """.stripMargin.format(testTable)).futureValue
  }

  def insertSampleData(): Unit = {
    val insertDataQuery =
      client.executeUpdate(
        """
          |INSERT INTO %s(str_field, int_field, double_field, timestamp_field, bool_field) VALUES
          | ('hello', 1234, 10.5, '2015-01-08 11:55:12-08:00', TRUE),
          | ('hello', 5557, -4.51, '2015-01-08 12:55:12-08:00', TRUE),
          | ('hello', 7787, -42.51, '2013-12-24 07:01:00-08:00', FALSE),
          | ('hello', null, null, '2015-02-24 07:01:00-08:00', null),
          | ('goodbye', 4567, 15.8, '2015-01-09 16:55:12+05:00', FALSE)
        """.stripMargin.format(testTable))

    val response = insertDataQuery.futureValue

    response mustEqual OK(5)
  }

  test("insert and select rows") {
    cleanDb()
    insertSampleData()

    val resultRows = client
      .select(
        "SELECT * FROM %s WHERE str_field='hello' ORDER BY timestamp_field".format(testTable)
      )(identity)
      .futureValue

    resultRows.size mustEqual 4

    val firstRow = resultRows.head

    firstRow.getOption[String]("str_field") must equal(Some("hello"))
    firstRow.get[Long]("int_field") must equal(7787)
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

  test("insert and select rows by id") {
    cleanDb()

    val resultRows = client
      .prepareAndQuery(
        s"""INSERT INTO $testTable(str_field, int_field, double_field, timestamp_field, bool_field)
              VALUES ('string_key', 9012, 15.8, '2015-01-09 16:55:12+05:00', FALSE)
              RETURNING *"""
      )(identity)
      .futureValue

    resultRows.size mustEqual 1

    val firstRow = resultRows.head

    val id = firstRow.get[Long]("id")

    val selectedById = client
      .prepareAndQuery("SELECT * FROM %s WHERE id=?".format(testTable), id)(identity)
      .futureValue

    selectedById.size mustEqual 1
    selectedById.head.get[Long]("id") mustEqual id
  }

  test("update row") {
    cleanDb()
    insertSampleData()

    val updateQueryResponse = client
      .executeUpdate(
        "UPDATE %s SET str_field='hello_updated' where int_field=4567".format(testTable)
      )
      .futureValue

    updateQueryResponse must equal(OK(1))

    val resultRows = client
      .select(
        "SELECT * FROM %s WHERE str_field='hello_updated'".format(testTable)
      )(identity)
      .futureValue

    resultRows.size must equal(1)
    resultRows.head.getOption[String]("str_field") must equal(Some("hello_updated"))
  }

  test("delete rows") {
    cleanDb()
    insertSampleData()

    val response = client
      .executeUpdate(
        "DELETE FROM %s WHERE str_field='hello'"
          .format(testTable)
      )
      .futureValue

    response must equal(OK(4))

    val resultRows = client
      .select(
        "SELECT * FROM %s".format(testTable)
      )(identity)
      .futureValue

    resultRows.size must equal(1)
    resultRows.head.getOption[String]("str_field") must equal(Some("goodbye"))
  }

  test("select rows via prepared query") {
    cleanDb()
    insertSampleData()

    val resultRows = client
      .prepareAndQuery("SELECT * FROM %s WHERE str_field=? AND bool_field=?".format(testTable),
                       "hello",
                       true)(identity)
      .futureValue

    resultRows.size must equal(2)
    resultRows.foreach { row =>
      row.getOption[String]("str_field") must equal(Some("hello"))
      row.getOption[Boolean]("bool_field") must equal(Some(true))
    }
  }

  test("execute an update via a prepared statement") {
    cleanDb()
    insertSampleData()

    val numRows = client
      .prepareAndExecute(
        "UPDATE %s SET str_field = ?, timestamp_field = ? where int_field = 4567".format(testTable),
        "hello_updated",
        Instant.now()
      )
      .futureValue

    val resultRows = client
      .select(
        "SELECT * from %s WHERE str_field = 'hello_updated' AND int_field = 4567".format(testTable)
      )(identity)
      .futureValue

    resultRows.size must equal(numRows)
  }

  test("execute an update via a prepared statement using a Some(value)") {
    cleanDb()
    insertSampleData()

    val numRows = client
      .prepareAndExecute(
        "UPDATE %s SET str_field = ? where int_field = 4567".format(testTable),
        Some("hello_updated_some")
      )
      .futureValue

    val resultRows = client
      .select(
        "SELECT * from %s WHERE str_field = 'hello_updated_some' AND int_field = 4567".format(
          testTable)
      )(identity)
      .futureValue

    resultRows.size must equal(numRows)
  }

  test("execute an update via a prepared statement using a None") {
    cleanDb()
    insertSampleData()

    val numRows = client
      .prepareAndExecute(
        "UPDATE %s SET str_field = ? where int_field = 4567".format(testTable),
        None: Option[String]
      )
      .futureValue

    val resultRows = client
      .select(
        "SELECT * from %s WHERE str_field IS NULL AND int_field = 4567".format(testTable)
      )(identity)
      .futureValue

    resultRows.size must equal(numRows)
  }

  test("return rows from UPDATE...RETURNING") {
    cleanDb()
    insertSampleData()

    val resultRows = client
      .prepareAndQuery(
        "UPDATE %s SET str_field = ? where int_field = 4567 RETURNING *".format(testTable),
        "hello_updated"
      )(identity)
      .futureValue

    resultRows.size must equal(1)
    resultRows.head.get[String]("str_field") must equal("hello_updated")
  }

  test("return rows from DELETE...RETURNING") {
    cleanDb()
    insertSampleData()

    client
      .prepareAndExecute(
        s"""INSERT INTO $testTable(str_field, int_field, double_field, timestamp_field, bool_field)
              VALUES ('delete', 9012, 15.8, '2015-01-09 16:55:12+05:00', FALSE)"""
      )
      .futureValue

    val resultRows = client
      .prepareAndQuery(
        "DELETE FROM %s where int_field = 9012 RETURNING *".format(testTable)
      )(identity)
      .futureValue

    resultRows.size must equal(1)
    resultRows.head.get[String]("str_field") must equal("delete")
  }

  test("execute an UPDATE...RETURNING that updates nothing") {
    cleanDb()
    insertSampleData()
    val resultRows = client
      .prepareAndQuery(
        "UPDATE %s SET str_field = ? where str_field = ? RETURNING *".format(testTable),
        "hello_updated",
        "xxxx"
      )(identity)
      .futureValue

    resultRows.size must equal(0)
  }

  test("execute a DELETE...RETURNING that deletes nothing") {

    cleanDb()
    insertSampleData()

    val resultRows = client
      .prepareAndQuery(
        "DELETE FROM %s WHERE str_field=?".format(testTable),
        "xxxx"
      )(identity)
      .futureValue

    resultRows.size must equal(0)
  }

  test("support UUID data type") {
    client
      .query(
        """
                   |CREATE TABLE test.uuid_table(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name STRING);
                 """.stripMargin)
      .futureValue

    val resultRows = client
      .prepareAndQuery("INSERT INTO test.uuid_table(name) VALUES ('a'), ('b'), ('c') RETURNING *")(
        identity)
      .futureValue

    resultRows.size must equal(3)

    val uuid = UUID.randomUUID()
    client
      .prepareAndExecute("INSERT INTO test.uuid_table(id, name) VALUES (?, ?)", uuid, "test_name")
      .futureValue

    val fetched = client
      .prepareAndQuery("select * from test.uuid_table where id = ?", uuid)(identity)
      .futureValue
    fetched.size must equal(1)
    fetched.head.get[UUID]("id") mustEqual uuid
    fetched.head.get[String]("name") mustEqual "test_name"
  }

  test("support multi-statement DDL") {
    client.query("""
        |CREATE TABLE test.multi_one(id integer);
        |CREATE TABLE test.multi_two(id integer);
        |DROP TABLE test.multi_one;
        |DROP TABLE test.multi_two;
      """.stripMargin).futureValue
  }
}
