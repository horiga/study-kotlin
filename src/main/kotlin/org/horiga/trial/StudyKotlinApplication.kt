package org.horiga.trial

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import org.horiga.trial.repository.converter.Converters
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@SpringBootApplication
class StudyKotlinApplication

fun main(args: Array<String>) {
    runApplication<StudyKotlinApplication>(*args)
}

/**
 * refs: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-exception-handler/index.html
 */
class LoggingCoroutineExceptionHandler : CoroutineExceptionHandler,
                                         AbstractCoroutineContextElement(CoroutineExceptionHandler) {

    companion object {
        val log = LoggerFactory.getLogger(LoggingCoroutineExceptionHandler::class.java)!!
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        log.error("UNCAUGHT EXCEPTION.", exception)
    }
}

@Configuration
class R2dbcConfig(val props: R2dbcProperties) : AbstractR2dbcConfiguration() {

    @Bean
    override fun connectionFactory(): ConnectionFactory {
        return ConnectionPool(
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
    }

    override fun getCustomConverters(): MutableList<Any> {
        return Converters.converters()
    }
}