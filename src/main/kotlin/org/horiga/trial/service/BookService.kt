@file:Suppress("SqlDialectInspection")

package org.horiga.trial.service

import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.horiga.trial.getOrThrow
import org.horiga.trial.handler.BookHandler
import org.horiga.trial.repository.BookEntity
import org.horiga.trial.repository.CoroutineBookRepository
import org.horiga.trial.repository.CoroutinePublisherRepository
import org.horiga.trial.repository.PublisherEntity
import org.horiga.trial.stringOrThrow
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.random.Random

data class Book(
    val id: String,
    val name: String,
    val publisherId: String,
    val publisherName: String,
    val registrationDate: Instant
) {
    companion object {
        fun from(row: Row) = Book(
            row.stringOrThrow("id"),
            row.stringOrThrow("name"),
            row.stringOrThrow("publisher_id"),
            row.stringOrThrow("publisher_name"),
            row.getOrThrow("registration_date", Instant::class.java)
        )
    }
}

@Service
class BookService(
    val bookRepository: CoroutineBookRepository,
    val publisherRepository: CoroutinePublisherRepository,
    val databaseClient: DatabaseClient
) {
    suspend fun findById(id: String): Book? =
        databaseClient.sql(
            """
            SELECT 
              t1.id as id, 
              t1.name as name, 
              t2.id as publisher_id, 
              t2.name as publisher_name, 
              t1.registration_date as registration_date
            FROM book t1 LEFT JOIN publisher t2 ON t1.publisher_id = t2.id WHERE t1.id = :id LIMIT 1
        """.trimIndent()
        )
            .bind("id", id).map { row -> Book.from(row) }.awaitSingleOrNull()

    suspend fun allBooks(): Flow<Book> =
        databaseClient.sql(
            """
            SELECT 
              t1.id as id, 
              t1.name as name, 
              t2.id as publisher_id, 
              t2.name as publisher_name, 
              t1.registration_date as registration_date
            FROM book t1 LEFT JOIN publisher t2 ON t1.publisher_id = t2.id 
            ORDER BY t1.registration_date DESC 
        """
        ).map { row -> Book.from(row) }.flow()

    @Transactional
    suspend fun addBook(book: BookHandler.AddBookRequest): Boolean {

        log.info("step-1(started)")

        if (bookRepository.existsById(book.id)) {
            return false
        }

        log.info("step-2")

        val publisher = publisherRepository.findFirstByNameOrderByIdDesc(book.publisherName)
        val publisherId = if (publisher != null) {
            log.info("Find exists publisher. publisher={}", publisher)
            publisher.id
        } else {
            "P${Random.nextInt(10, 3000)}".let {
                publisherRepository.insert(it, book.publisherName)
                it
            }
        }

        log.info("step-2-1")

        // Rollback test
        if (book.name == "ERROR") throw IllegalStateException("Fire runtime error!!")

        log.info("step-3")

        bookRepository.insert(book.id, book.name, publisherId!!)

        log.info("step-4(finished)")

        return true
    }

    companion object { val log = LoggerFactory.getLogger(BookService::class.java)!! }
}