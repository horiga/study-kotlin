package org.horiga.trial.repository

import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.Instant

@Table("publisher")
data class PublisherEntity(
    @Id
    val id: String?,
    @Column("name")
    val name: String,
    @Column("registration_date")
    val registrationDate: Instant,
)

@Suppress("SqlDialectInspection")
interface CoroutinePublisherRepository : CoroutineCrudRepository<PublisherEntity, String> {

    @Modifying
    @Query(
        """
        insert into publisher(id, name, registration_date)
        values(:id, :name, :registrationDate)
    """
    )
    suspend fun insert(id: String, name: String, registrationDate: Instant = Instant.now())

    @Suppress("SpringDataRepositoryMethodReturnTypeInspection")
    suspend fun findFirstByNameOrderByIdDesc(name: String): PublisherEntity?
}