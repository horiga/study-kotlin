package org.horiga.trial.repository

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.data.annotation.Id
import org.springframework.data.convert.ConverterBuilder
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

interface TypeValue {
    val enumValue: String
    val displayValue: String
}

enum class SampleType(
    override val enumValue: String, // database record value
    override val displayValue: String
) : TypeValue {
    ADMIN("A", "Administrator"),
    GENERAL("G", "General"),
    INDIVIDUAL("I", "Individual");

    companion object {

        // FIXME: fun converters(): Set<Any> = Converters.enumTypeConverters(SampleType::class.java)

        fun converters(): Set<GenericConverter> =
            ConverterBuilder.writing(SampleType::class.java, String::class.java) { type -> type.enumValue }
                .andReading { s -> values().find { it.enumValue == s } }.converters

        @JsonCreator
        fun fromJson(value: String): SampleType = values().find { it.name.equals(value, true) }
            ?: throw IllegalArgumentException("Not one of the values accepted. source=$value")
    }

    @JsonValue
    fun toJson() = this.name
}

// for testing enum, boolean, binary convert
@Table("sample")
data class SampleEntity(
    @Id
    var id: UUID,
    @Column("type")
    var type: SampleType,
    @Column("enabled")
    var enabled: Boolean,
    @Column("number")
    var number: Int? = null,
    @Column("memo")
    var memo: String? = null,
    @Column("registration_date")
    var registrationDate: Instant
)

@Repository
class CoroutineSampleRepository(
    val databaseClient: DatabaseClient,
    val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    companion object {
        val log = LoggerFactory.getLogger(CoroutineSampleRepository::class.java)!!
    }

    suspend fun insert(entity: SampleEntity): UUID? {
        log.info(">> INSERT: {}", entity)
        return r2dbcEntityTemplate.insert(entity).map { entity.id }.awaitSingle()
    }

    suspend fun findById(id: UUID): SampleEntity =
        r2dbcEntityTemplate.selectOne(
            Query.query(Criteria.where("id").`is`(id)),
            SampleEntity::class.java
        ).awaitSingle()

    suspend fun findAll(): Flow<SampleEntity> = r2dbcEntityTemplate.select<SampleEntity>().flow()
}