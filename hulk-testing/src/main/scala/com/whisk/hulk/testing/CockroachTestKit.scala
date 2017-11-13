package com.whisk.hulk.testing

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import com.github.mauricio.async.db.Configuration
import com.whisk.hulk.HulkClient
import org.scalatest.Suite
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

trait CockroachTestKit extends DockerCockroachService { self: Suite =>

  private val log = LoggerFactory.getLogger(classOf[CockroachTestKit])

  protected val hulkClient: AtomicReference[HulkClient] = new AtomicReference[HulkClient]()

  protected val hulkClientLatch = new CountDownLatch(1)

  def hulkInitSchemaPaths: Seq[String] = Seq()

  override def afterStart(): Unit = {
    super.afterStart()
    val port = cockroachContainer.mappedPort(CockroachAdvertisedPort)

    val client = createHulkClient()
    hulkClient.set(client)
    hulkClientLatch.countDown()

    if (hulkInitSchemaPaths.nonEmpty) {
      // need to create schemas
      log.info(s"initialising schemas for paths:")
      hulkInitSchemaPaths.foreach(p => log.info("  - " + p))

      val createSchemas: Seq[String] = hulkInitSchemaPaths.map { p =>
        Source.fromInputStream(this.getClass.getResourceAsStream(p)).mkString
      }

      log.info("all schema files loaded")

      createSchemas.foreach { s =>
        Await.result(client.query(s), 10.seconds)
      }

      log.info("schemas created")
    }
  }

  protected def createHulkClient(): HulkClient = {
    val host = dockerClient.getHost
    val port = cockroachContainer.mappedPort(CockroachAdvertisedPort)
    val conf = Configuration(
      username = CockroachUser,
      host = host,
      port = port,
      testTimeout = 10.seconds
    )
    HulkClient.fromAsyncPool(5, conf)
  }
}
