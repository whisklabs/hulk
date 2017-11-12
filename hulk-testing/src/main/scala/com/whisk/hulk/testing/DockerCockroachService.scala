package com.whisk.hulk.testing

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import com.whisk.docker.testkit.{ContainerSpec, DockerReadyChecker, ManagedContainers}
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerCockroachService extends DockerTestKitForAll { self: Suite =>

  def PostgresAdvertisedPort = 26257
  val PostgresUser = "root"
  val PostgresPassword = None
  val PostgresDatabase = "test"

  protected val postgresContainer = ContainerSpec("cockroachdb/cockroach:v1.1.2")
    .withExposedPorts(PostgresAdvertisedPort)
    .withReadyChecker(
      DockerReadyChecker
        .Jdbc(
          driverClass = "org.postgresql.Driver",
          user = PostgresUser,
          password = PostgresPassword,
          database = None,
          port = Some(PostgresAdvertisedPort)
        )
        .looped(25, 1.second)
    )
    .withCommand("start", "--insecure")
    .toContainer

  override val managedContainers: ManagedContainers = postgresContainer.toManagedContainer
}
