@file:Suppress("SqlDialectInspection")

package org.horiga.trial.service

import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import org.horiga.trial.getOrThrow
import org.horiga.trial.handler.BookHandler
import org.horiga.trial.repository.CoroutineCrudBookRepository
import org.horiga.trial.repository.CoroutineCrudPublisherRepository
import org.horiga.trial.stringOrThrow
import org.horiga.trial.withR2dbcContext
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
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
    val bookRepository: CoroutineCrudBookRepository,
    val publisherRepository: CoroutineCrudPublisherRepository,
    val databaseClient: DatabaseClient
) {
    suspend fun findById(id: String): Book? {
        log.info(">> findById::start")
        return withR2dbcContext {
            log.info(">> findById::execute")
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
            ).bind("id", id).map { row -> Book.from(row) }.awaitSingleOrNull()
        }
    }

    suspend fun allBooks(): Flow<Book> {
        log.info(">> allBooks::start")
        return withR2dbcContext {
            log.info(">> service::allBooks::execute")
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
        }
    }

    @Transactional
    suspend fun addBook(book: BookHandler.AddBookRequest): Boolean {

        log.info("step-1(started)")

        val exists = withR2dbcContext {
            log.info("step-2(default-context)")
            bookRepository.existsById(book.id)
        }
        if (exists) {
            return false
        }

        log.info("step-3")

        val publisher = withR2dbcContext {
            log.info("step-4(default-context)")
            publisherRepository.findFirstByNameOrderByIdDesc(book.publisherName)
        }

        val publisherId = if (publisher != null) {
            log.info("step-5")
            publisher.id
        } else {
            "P${Random.nextInt(10, 3000)}".let {
                withR2dbcContext {
                    log.info("step-5(default-context)")
                    publisherRepository.insert(it, book.publisherName)
                }
                it
            }
        }

        log.info("step-6")

        // Rollback test
        if (book.name == "ERROR") throw IllegalStateException("Fire runtime error!!")

        withR2dbcContext {
            log.info("step-7(default-context)")
            bookRepository.insert(book.id, book.name, publisherId!!)
        }

        log.info("step-8(finished)")

        return true
    }

    companion object { val log = LoggerFactory.getLogger(BookService::class.java)!! }
}