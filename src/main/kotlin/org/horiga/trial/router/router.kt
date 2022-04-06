package org.horiga.trial.router

import org.horiga.trial.handler.BookHandler
import org.horiga.trial.handler.SampleHandler
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFilterFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Mono
import java.util.UUID

@Configuration
class RouterConfig(
    val bookHandler: BookHandler,
    val sampleHandler: SampleHandler
) {
    companion object {
        val log = LoggerFactory.getLogger(RouterConfig::class.java)!!
    }

    @Bean
    fun routes(defaultRequestFilter: HandlerFilterFunction<ServerResponse, ServerResponse>) = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            GET("/hello") { req ->
                log.info("Request attributes: request_id={}", req.attribute("request_id"))
                ServerResponse.ok().bodyValueAndAwait("Hello, ${req.queryParam("name").orElse("World")}!")
            }
        }
        accept(MediaType.APPLICATION_JSON).nest {
            GET("/books", bookHandler::findAll)
            GET("/books/{id}", bookHandler::findById)
            POST("/books", bookHandler::addBook)
        }

        accept(MediaType.APPLICATION_JSON).nest {
            GET("/samples", sampleHandler::findAll)
            GET("/samples/{id}", sampleHandler::findById)
            POST("/samples", sampleHandler::add)
        }

    }.filter(defaultRequestFilter)

    @Bean
    fun defaultRequestFilter(): HandlerFilterFunction<ServerResponse, ServerResponse> =
        HandlerFilterFunction { request, next ->
            log.info("Filter fn: {} {}", request.method(), request.path())

            request.headers().header("x-study-debug")
                .find { it.equals("forbidden", true) }?.let {
                    return@HandlerFilterFunction ServerResponse.status(HttpStatus.FORBIDDEN).build()
                }

            // test for add attributes
            val reqId = request.headers().header("x-request-id")
                .takeIf { it.isNotEmpty() }?.get(0) ?: UUID.randomUUID().toString()
            request.attributes()["request_id"] = reqId

            next.handle(request)
        }

    @Component
    class GlobalExceptionHandler(
        errorAttributes: ErrorAttributes?,
        // 2.6+: Injecting Resources directly no longer works as this configuration has been harmonized in WebProperties.
        // refs: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.6-Release-Notes#web-resources-configuration
        webProperties: WebProperties,
        applicationContext: ApplicationContext?,
        serverCodecConfigurer: ServerCodecConfigurer
    ) : AbstractErrorWebExceptionHandler(errorAttributes, webProperties.resources, applicationContext) {

        companion object {
            val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)!!
        }

        init {
            this.setMessageWriters(serverCodecConfigurer.writers)
        }

        override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse> =
            RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)

        fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
            val cause = getError(request)

            val (status, message) = when (cause) {
                is IllegalArgumentException -> Pair(HttpStatus.BAD_REQUEST, cause.message)
                is NoSuchElementException -> Pair(HttpStatus.NOT_FOUND, cause.message)
                is EmptyResultDataAccessException -> Pair(HttpStatus.NOT_FOUND, cause.message)
                else -> Pair(HttpStatus.INTERNAL_SERVER_ERROR, "unknown error")
            }

            log.error(
                "handle global exception. {} {}, status={}, message={}",
                request.method(), request.path(),
                status, message, cause
            )

            return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(mapOf("error_message" to message)))
        }
    }
}