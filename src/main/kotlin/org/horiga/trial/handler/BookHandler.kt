package org.horiga.trial.handler

import kotlinx.coroutines.flow.Flow
import org.horiga.trial.service.BookService
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
        suspend inline fun <reified T : Any> success(flow: Flow<T>): ServerResponse =
            ServerResponse.ok().bodyAndAwait(flow)

        suspend inline fun success(body: Any): ServerResponse =
            ServerResponse.ok().bodyValueAndAwait(body)
    }

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