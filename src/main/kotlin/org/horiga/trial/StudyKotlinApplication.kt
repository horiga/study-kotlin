package org.horiga.trial

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
import kotlinx.coroutines.CoroutineExceptionHandler
import org.horiga.trial.repository.converter.Converters
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import java.time.Duration
import javax.validation.Validation
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@SpringBootApplication
class StudyKotlinApplication {
    companion object {
        val globalValidator = Validation.buildDefaultValidatorFactory().validator
    }
}

fun main(args: Array<String>) {

    // default coroutines Dispatchers.IO parallelism 64 'kotlinx.coroutines.io.parallelism=64'
    System.setProperty("kotlinx.coroutines.io.parallelism", "512")

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

