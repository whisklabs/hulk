package com.whisk.hulk.tests

import java.util.UUID

import com.whisk.hulk.Row
import com.whisk.hulk.testing.CockroachTestKit
import io.circe.{Decoder, Json, JsonObject}
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuite, MustMatchers}
import com.whisk.hulk.circe._
import scala.concurrent.ExecutionContext.Implicits.global

class CirceDecodersTest extends FunSuite with MustMatchers with CockroachTestKit with ScalaFutures {

  case class Recipe(id: String, name: String)

  case class RecipeMetadata(cuisine: Option[String] = None, mealType: Option[String] = None)

  private implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  private lazy val client = hulkClient.get()

  private def createTable() = {
    client.query("""
        |CREATE DATABASE IF NOT EXISTS test;
        |CREATE TABLE test.recipes (
        |  id   VARCHAR(36) PRIMARY KEY,
        |  name VARCHAR(128) NOT NULL,
        |  tags VARCHAR(128),
        |  data STRING
        |)
      """.stripMargin).futureValue
  }

  test("decode circe types") {
    createTable()

    val recipe = Recipe(UUID.randomUUID().toString, "Salmon pasta salad with lemon & capers")

    val json = Json.obj("mealType" := "Lunch")

    client
      .prepareAndExecute("insert into test.recipes(id, name, data) values (?, ?, ?)",
                         recipe.id,
                         recipe.name,
                         json.noSpaces)
      .futureValue mustEqual 1

    val row: Row = client
      .prepareAndQuery("select data from test.recipes where id = ?", recipe.id)(identity)
      .map(_.head)
      .futureValue

    row.get[Json]("data") mustEqual json
    row.get[JsonObject]("data") mustEqual json.asObject.get

    val expectedValue = RecipeMetadata(mealType = Some("Lunch"))

    implicit val metadataCirceDecoder: Decoder[RecipeMetadata] =
      Decoder.forProduct2("cuisine", "mealType")(RecipeMetadata.apply)

    row.json[RecipeMetadata]("data") mustBe expectedValue
  }

}
