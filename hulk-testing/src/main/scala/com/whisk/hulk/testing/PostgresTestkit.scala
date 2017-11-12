package com.whisk.hulk.testing

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.whisk.hulk.PostgresClient
import org.scalatest.Suite
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

trait PostgresTestkit extends DockerCockroachService { self: Suite =>

  private val log = LoggerFactory.getLogger(classOf[PostgresTestkit])

  protected val pgClient: AtomicReference[PostgresClient] = new AtomicReference[PostgresClient]()

  protected val pgClientLatch = new CountDownLatch(1)

  def pgInitSchemaPaths: Seq[String] = Seq()

  override def afterStart(): Unit = {
    super.afterStart()
    val port = postgresContainer.mappedPort(PostgresAdvertisedPort)

    val client = createClient()
    pgClient.set(client)
    pgClientLatch.countDown()

    if (pgInitSchemaPaths.nonEmpty) {
      // need to create schemas
      log.info(s"initialising schemas for paths:")
      pgInitSchemaPaths.foreach(p => log.info("  - " + p))

      val createSchemas: Seq[String] = pgInitSchemaPaths.map { p =>
        Source.fromInputStream(this.getClass.getResourceAsStream(p)).mkString
      }

      log.info("all schema files loaded")

      createSchemas.foreach { s =>
        Await.result(client.query(s), 10.seconds)
      }

      log.info("schemas created")
    }
  }

  protected def createClient(): PostgresClient = {
    val host = dockerClient.getHost
    val port = postgresContainer.mappedPort(PostgresAdvertisedPort)
    val conf = Configuration(
      username = PostgresUser,
      host = host,
      port = port,
      password = PostgresPassword
    )
    val connection = new PostgreSQLConnection(conf)
    Await.result(connection.connect, 5.seconds)
    PostgresClient.from(connection)
  }
}
