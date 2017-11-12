package com.whisk.hulk.cockroach

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.util.{ExecutorServiceUtils, NettyUtils}
import io.netty.channel.EventLoopGroup

import scala.concurrent.{Await, ExecutionContext}

class CockroachConnectionFactory(override val configuration: Configuration,
                                 group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
                                 executionContext: ExecutionContext =
                                   ExecutorServiceUtils.CachedExecutionContext)
    extends PostgreSQLConnectionFactory(configuration, group, executionContext) {
  override def create: PostgreSQLConnection = {
    val connection =
      new PostgreSQLConnection(configuration,
                               group = group,
                               executionContext = executionContext,
                               decoderRegistry = CockroachColumnDecoderRegistry.Instance)
    Await.result(connection.connect, configuration.connectTimeout)
    connection
  }
}
