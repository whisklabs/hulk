package com.whisk.hulk.testing

import com.whisk.docker.testkit.ContainerState
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class PostgresTestkitTest extends FunSuite with PostgresTestkit {

  test("test container started") {
    assert(postgresContainer.state().isInstanceOf[ContainerState.Ready], "postgres is ready")
    assert(postgresContainer.mappedPortOpt(PostgresAdvertisedPort).isDefined,
           "postgres port exposed")
    val res = Await.result(pgClient.get().fetch("select 1"), 5.seconds)
    assert(res.rows.nonEmpty, "client should be connected")
  }
}
