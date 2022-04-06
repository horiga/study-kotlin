package org.horiga.trial.handler

import org.horiga.trial.awaitBody
import org.horiga.trial.repository.CoroutineSampleRepository
import org.horiga.trial.repository.SampleEntity
import org.horiga.trial.repository.SampleType
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.time.Instant
import java.util.UUID
import javax.validation.Valid
import javax.validation.Validator
import javax.validation.constraints.Size

@Component
class SampleHandler(
    val sampleRepository: CoroutineSampleRepository,
    val validator: Validator
) {

    @Validated
    data class RequestBody(
        var type: SampleType,
        var enabled: Boolean = true,
        @field:Size(max = 10)
        var memo: String? = null,
        var number: Int? = null
    )

    suspend fun add(@Valid req: ServerRequest): ServerResponse {
        val payload = req.awaitBody(RequestBody::class, validator)
        val id = sampleRepository.insert(
            SampleEntity(
                UUID.randomUUID(),
                payload.type,
                payload.enabled,
                payload.number,
                payload.memo,
                Instant.now()
            )
        )
        return ServerResponse.ok().bodyValueAndAwait(mapOf("id" to id))
    }

    suspend fun findById(req: ServerRequest): ServerResponse = req.pathVariable("id").let {
        ServerResponse.ok().bodyValueAndAwait(sampleRepository.findById(UUID.fromString(it)))
    }

    suspend fun findAll(req: ServerRequest) = ServerResponse.ok().bodyAndAwait(sampleRepository.findAll())
}