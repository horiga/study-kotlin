package org.horiga.trial.router

import kotlinx.coroutines.flow.Flow
import org.horiga.trial.repository.Book
import org.horiga.trial.repository.CoroutineBookRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono

@Configuration
class RouterConfig(
    val bookHandler: BookHandler
) {

    companion object {
        val log = LoggerFactory.getLogger(RouterConfig::class.java)!!
    }

    @Bean
    fun routes() = coRouter {

        accept(MediaType.APPLICATION_JSON).nest {
            GET("/hello") { req ->
                ServerResponse.ok().bodyValueAndAwait("Hello, ${req.queryParam("name").orElse("World")}!")
            }
            GET("/books") { _ ->
                log.info("router - /books")
                ServerResponse.ok().bodyAndAwait(bookHandler.findAll())
            }

            GET("/books/{id}") { req ->
                val id = req.pathVariable("id")
                log.info("router - /books/{}", id)
                ServerResponse.ok().bodyValueAndAwait(bookHandler.findById(id))
            }
        }
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
            RouterFunctions.route(RequestPredicates.all(), HandlerFunction { request -> renderErrorResponse(request) } )

        fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
            val cause = getError(request)
            val (status, message) = when (cause) {
                is NoSuchElementException -> Pair(HttpStatus.NOT_FOUND, cause.message)
                else -> Pair(HttpStatus.INTERNAL_SERVER_ERROR, "unknown error")
            }

            log.error("handle global exception. status={}, message={}", status, message, cause)

            return ServerResponse
                    .status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(mapOf("error_message" to message)))
        }
    }
}

@Service
class BookHandler(val bookRepository: CoroutineBookRepository) {

    suspend fun findAll(): Flow<Book> = bookRepository.findAll()

    suspend fun findById(id: String): Book = bookRepository.findById(id)
            ?: throw NoSuchElementException("Book is not founded. id=$id")
}