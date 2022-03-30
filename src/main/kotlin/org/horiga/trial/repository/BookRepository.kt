package org.horiga.trial.repository

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
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

@Suppress("SqlDialectInspection")
@Repository
interface CoroutineCrudBookRepository : CoroutineCrudRepository<BookEntity, String> {
    @Modifying
    @Query(
        """
        insert into book(id, name, publisher_id, registration_date)
        values(:id, :name, :publisherId, :registrationDate)
    """
    )
    suspend fun insert(id: String, name: String, publisherId: String, registrationDate: Instant = Instant.now())
}

// Not use/implement `CoroutineCrudRepository`
@Repository
class CoroutineBookRepository(
    val databaseClient: DatabaseClient,
    val r2dbcEntityTemplate: R2dbcEntityTemplate
) {

    suspend fun insert(entity: BookEntity) = r2dbcEntityTemplate.insert(entity).awaitSingle()

    suspend fun findAll() {

    }

    // TODO: develop
}