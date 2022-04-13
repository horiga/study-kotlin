package org.horiga.trial.handler

import kotlinx.coroutines.flow.Flow
import org.horiga.trial.requestId
import org.horiga.trial.service.BookService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Component
class BookHandler(val bookService: BookService) {

    companion object {
        val log = LoggerFactory.getLogger(BookHandler::class.java)!!

        suspend inline fun <reified T : Any> success(flow: Flow<T>): ServerResponse =
            ServerResponse.ok().bodyAndAwait(flow)

        suspend inline fun success(body: Any): ServerResponse =
            ServerResponse.ok().bodyValueAndAwait(body)
    }

    // TODO: bean validation ?
    data class AddBookRequest(
        @field:NotBlank
        @field:Size(max = 20)
        var id: String,
        @field:NotBlank
        @field:Size(max = 200)
        var name: String,
        @field:NotBlank
        @field:Size(max = 200)
        var publisherName: String
    )

    suspend fun findAll(req: ServerRequest): ServerResponse = success(bookService.allBooks())

    suspend fun findById(req: ServerRequest): ServerResponse {
        log.info("bookHandler. request_id: {}", req.requestId())
        val id = req.pathVariable("id")
        return success(
            bookService.findById(id)
                ?: throw NoSuchElementException("Book is not founded. id=$id")
        )
    }

    suspend fun addBook(req: ServerRequest): ServerResponse {
        bookService.addBook(req.awaitBody(AddBookRequest::class))
        return ServerResponse.ok().buildAndAwait()
    }
}