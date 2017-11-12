package com.whisk.hulk.testing

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import com.whisk.docker.testkit.{ContainerSpec, DockerReadyChecker, ManagedContainers}
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerPostgresService extends DockerTestKitForAll { self: Suite =>

  def PostgresAdvertisedPort = 5432
  val PostgresUser = "test"
  val PostgresPassword = Some("test")
  val PostgresDatabase = "test"

  protected val postgresContainer = ContainerSpec("quay.io/whisk/fastboot-postgres:9.6.5")
    .withExposedPorts(PostgresAdvertisedPort)
    .withReadyChecker(
      DockerReadyChecker
        .Jdbc(
          driverClass = "org.postgresql.Driver",
          user = PostgresUser,
          password = PostgresPassword,
          database = Some(PostgresDatabase),
          port = Some(PostgresAdvertisedPort)
        )
        .looped(25, 1.second)
    )
    .toContainer

  override val managedContainers: ManagedContainers = postgresContainer.toManagedContainer
}
