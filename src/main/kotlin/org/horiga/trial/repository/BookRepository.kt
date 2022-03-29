package org.horiga.trial.repository

import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Table("book")
data class BookEntity(
    @Id
    val id: String,
    @Column("name")
    val name: String,
    @Column("registration_date")
    val registrationDate: Instant
)

@Repository
interface BookRepository : ReactiveCrudRepository<BookEntity, String>

@Suppress("SqlDialectInspection")
@Repository
interface CoroutineBookRepository : CoroutineCrudRepository<BookEntity, String> {
    @Modifying
    @Query(
        """
        insert into book(id, name, publisher_id, registration_date)
        values(:id, :name, :publisherId, :registrationDate)
    """
    )
    suspend fun insert(id: String, name: String, publisherId: String, registrationDate: Instant = Instant.now())
}

