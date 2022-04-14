package org.horiga.trial.config

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.proxy.ProxyConnectionFactory
import io.r2dbc.proxy.core.QueryExecutionInfo
import io.r2dbc.proxy.listener.ProxyExecutionListener
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter
import io.r2dbc.spi.ConnectionFactory
import org.horiga.trial.repository.converter.Converters
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import java.time.Duration

@Configuration
class R2dbcConfig(
    val props: R2dbcProperties,
    val meterRegistry: MeterRegistry
) : AbstractR2dbcConfiguration() {

    // refs: https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/listener-example/src/main/java/io/r2dbc/examples/MetricsExecutionListener.java
    class R2dbcMetricsExecutionListener(
        val meterRegistry: MeterRegistry,
        val tags: Collection<Tag> = emptyList(),
        val slowQueryMillis: Long = 1000L,
    ) : ProxyExecutionListener {

        companion object {
            val log = LoggerFactory.getLogger(R2dbcMetricsExecutionListener::class.java)!!
        }

        private val metricsNamePrefix = "r2dbc_"

        private val slowQueryThreshold = Duration.ofMillis(slowQueryMillis)!!

        /**
         * Full format
         * xxxxxxx: Time:26 Connection:3 Query:["
        SELECT
        t1.id as id,
        t1.name as name,
        t2.id as publisher_id,
        t2.name as publisher_name,
        t1.registration_date as registration_date
        FROM book t1 LEFT JOIN publisher t2 ON t1.publisher_id = t2.id
        ORDER BY t1.registration_date DESC
        "] Type:Statement Success:True Transaction:{Create:0 Rollback:0 Commit:0}
         */
        val queryFormat = QueryExecutionInfoFormatter()
            .showTime()
            .showConnection()
            .showQuery()
        //.showType() // opt
        //.showSuccess() // opt
        //.showTransaction() // opt

        override fun afterQuery(execution: QueryExecutionInfo) {
            log.info("query:\n {}", queryFormat.format(execution))

            increment("query", execution)
            record(execution)

            if (slowQueryThreshold.minus(execution.executeDuration).isNegative) {
                increment("slow_query", execution)
            }
        }

        fun increment(
            name: String,
            execution: QueryExecutionInfo
        ) {
            val query = when {
                execution.queries.size > 1 -> "BATCH"
                else -> {
                    execution.queries.first().query.let { query ->
                        val q = query.replace("\n", "").trim()
                        log.info("QUERY: {}", q)
                        q.split(" ")[0].uppercase().trim().takeIf {
                            listOf("SELECT", "INSERT", "UPDATE", "DELETE").contains(it)
                        } ?: "UNDEFINED"
                    }
                }
            }
            Counter.builder("${metricsNamePrefix}$name")
                .tags(tags)
                .tag("method", execution.method.name)
                .tag("query", query)
                .tag("result", if (execution.isSuccess) "success" else "fail")
                .tag("type", execution.type.name)
                .description("Number of executed queries. ($name)")
                .register(meterRegistry)
                .increment()
        }

        fun record(execution: QueryExecutionInfo) {
            Timer.builder("${metricsNamePrefix}query_duration")
                //.publishPercentileHistogram()
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .tags(tags)
                .tag("method", execution.method.name)
                .tag("result", if (execution.isSuccess) "success" else "fail")
                .tag("type", execution.type.name)
                .description("Duration of executed queries")
                .register(meterRegistry)
                .record(execution.executeDuration)
        }
    }

    @Bean
    override fun connectionFactory(): ConnectionFactory {

        val cf = ConnectionPool(
            ConnectionPoolConfiguration.builder()
                .connectionFactory(
                    ConnectionFactoryBuilder.withUrl(props.url)
                        .username(props.username)
                        .password(props.password)
                        .build()
                )
                .maxIdleTime(props.pool.maxIdleTime)
                .initialSize(props.pool.initialSize)
                .maxCreateConnectionTime(props.pool.maxCreateConnectionTime)
                .maxSize(props.pool.maxSize)
                .validationQuery("select 1")
                .build()
        )

        return ProxyConnectionFactory.builder(cf).listener(
            R2dbcMetricsExecutionListener(
                meterRegistry,
                listOf(Tag.of("host", "default"))
            )
        ).build()
    }

    override fun getCustomConverters(): MutableList<Any> {
        return Converters.converters()
    }
}