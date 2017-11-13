package com.whisk.hulk.cockroach

import java.util
import java.util.concurrent.ForkJoinPool

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.util.ExecutorServiceUtils
import com.github.mauricio.async.db.{Configuration, Connection}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

object AsyncConnectionPool {
  private val logger: Logger = LoggerFactory.getLogger(classOf[AsyncConnectionPool])
}

class AsyncConnectionPool(
    val maxPoolSize: Int,
    val configuration: Configuration,
    ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())) {

  private var poolSize: Int = 0
  final private val availableConnections: util.Deque[Connection] = new util.ArrayDeque[Connection]
  final private val waiters: util.Deque[Promise[Connection]] =
    new util.ArrayDeque[Promise[Connection]]

  protected def create(): Connection =
    new PostgreSQLConnection(configuration,
                             executionContext = ExecutorServiceUtils.CachedExecutionContext,
                             decoderRegistry = CockroachColumnDecoderRegistry.Instance)

  private def createConnection(): Future[Connection] = synchronized {
    poolSize += 1
    val promise = Promise[Connection]()
    try {
      val f = create().connect

      f.onComplete {
        case Failure(t) =>
          poolSize -= 1
          notifyWaitersAboutAvailableConnection()
        case _ =>
      }(ec)
      promise.completeWith(f)

    } catch {
      case e: Throwable =>
        AsyncConnectionPool.logger.info("creating a connection went wrong", e)
        poolSize -= 1
        promise.failure(e)
    }
    promise.future
  }

  private def waitForAvailableConnection(promise: Promise[Connection]): Unit = synchronized {
    waiters.add(promise)
  }

  private def createOrWaitForAvailableConnection(): Future[Connection] = synchronized {
    if (poolSize < maxPoolSize) createConnection()
    else {
      val promise = Promise[Connection]()
      waitForAvailableConnection(promise)
      promise.future
    }
  }

  def take(): Future[Connection] = synchronized {
    val connection: Connection = availableConnections.poll
    if (connection == null) createOrWaitForAvailableConnection()
    else if (connection.isConnected) Future.successful(connection)
    else {
      poolSize -= 1
      take()
    }
  }

  private def notifyWaitersAboutAvailableConnection(): Unit = synchronized {
    val handler: Promise[Connection] = waiters.poll
    if (handler != null) handler.completeWith(take())
  }

  def giveBack(connection: Connection): Unit = synchronized {
    if (connection.isConnected) availableConnections.add(connection)
    else poolSize -= 1
    notifyWaitersAboutAvailableConnection()
  }

  def close(): Unit = synchronized {
    availableConnections.forEach(_.disconnect)
  }
}
