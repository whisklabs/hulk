package com.whisk.hulk.testing

import com.whisk.docker.testkit.ContainerState
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class CockroachTestkitTest extends FunSuite with CockroachTestKit {

  test("test container started") {
    assert(cockroachContainer.state().isInstanceOf[ContainerState.Ready], "postgres is ready")
    assert(cockroachContainer.mappedPortOpt(CockroachAdvertisedPort).isDefined,
           "postgres port exposed")
    val res = Await.result(hulkClient.get().fetch("select 1"), 5.seconds)
    assert(res.rows.nonEmpty, "client should be connected")
  }
}
