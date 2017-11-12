package com.whisk.hulk.testing

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import com.whisk.docker.testkit.{ContainerSpec, DockerReadyChecker, ManagedContainers}
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerCockroachService extends DockerTestKitForAll { self: Suite =>

  def CockroachAdvertisedPort = 26257
  val CockroachUser = "root"
  val CockroachDatabase = "test"

  protected val cockroachContainer = ContainerSpec("cockroachdb/cockroach:v1.1.2")
    .withExposedPorts(CockroachAdvertisedPort)
    .withReadyChecker(
      DockerReadyChecker
        .Jdbc(
          driverClass = "org.postgresql.Driver",
          user = CockroachUser,
          password = None,
          database = None,
          port = Some(CockroachAdvertisedPort)
        )
        .looped(25, 1.second)
    )
    .withCommand("start", "--insecure")
    .toContainer

  override val managedContainers: ManagedContainers = cockroachContainer.toManagedContainer
}
