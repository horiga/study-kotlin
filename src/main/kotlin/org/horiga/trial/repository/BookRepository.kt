package org.horiga.trial.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.Instant

data class Book(
    @Id
    val id: String,
    @Column("name")
    val name: String,
    @Column("registration_date")
    val registrationDate: Instant
)

@Repository
interface BookRepository: ReactiveCrudRepository<Book, String> {
}

@Repository
interface CoroutineBookRepository: CoroutineCrudRepository<Book, String> {}

