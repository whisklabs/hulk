package com.whisk.hulk.testing

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.whisk.hulk.PostgresClient
import org.jdbi.v3.core.Jdbi
import org.scalatest.Suite
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

trait PostgresTestkit extends DockerPostgresService { self: Suite =>

  private val log = LoggerFactory.getLogger(classOf[PostgresTestkit])

  protected val pgClient: AtomicReference[PostgresClient] = new AtomicReference[PostgresClient]()

  protected val pgClientLatch = new CountDownLatch(1)

  def pgInitSchemaPaths: Seq[String] = Seq()

  override def afterStart(): Unit = {
    super.afterStart()
    val port = postgresContainer.mappedPort(PostgresAdvertisedPort)

    def initClient(): Unit = {
      pgClient.set(createClient())
      pgClientLatch.countDown()
    }

    // this block here is for performance optimisations of bootstrapping test
    // as Finagle Mysql client takes a while to init
    if (pgInitSchemaPaths.isEmpty) {
      // no need to create schemas -> initialising client in current thread
      initClient()
    } else {
      // need to create schemas
      log.info(s"initialising schemas for paths:")
      pgInitSchemaPaths.foreach(p => log.info("  - " + p))

      val createSchemas: Seq[String] = pgInitSchemaPaths.map { p =>
        Source.fromInputStream(this.getClass.getResourceAsStream(p)).mkString
      }

      log.info("all schema files loaded")

      // start initialising Postgres client in separate thread
      new Thread(() => {
        initClient()
      }).start()

      val jdbcUrl = s"jdbc:mysql://${dockerClient.getHost}:$port/test"
      val jdbi = Jdbi.create(jdbcUrl, PostgresUser, PostgresPassword)
      createSchemas.foreach { s =>
        jdbi.withHandle { h =>
          h.createScript(s).execute()
        }
      }

      log.info("schemas created")
    }

    pgClientLatch.await(20, TimeUnit.SECONDS)
  }

  protected def createClient(): PostgresClient = {
    val host = dockerClient.getHost
    val port = postgresContainer.mappedPort(PostgresAdvertisedPort)
    val conf = Configuration(
      username = PostgresUser,
      host = host,
      port = port,
      password = Some(PostgresPassword)
    )
    val connection = new PostgreSQLConnection(conf)
    Await.result(connection.connect, 5.seconds)
    new PostgresClient(connection)
  }
}
