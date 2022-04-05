package org.horiga.trial.handler

import org.horiga.trial.repository.CoroutineSampleRepository
import org.horiga.trial.repository.SampleEntity
import org.horiga.trial.repository.SampleType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.time.Instant
import java.util.UUID

@Component
class SampleHandler(val sampleRepository: CoroutineSampleRepository) {

    data class RequestBody(
        var type: SampleType,
        var enabled: Boolean = true,
        var memo: String? = null,
        var number: Int? = null
    )

    suspend fun add(req: ServerRequest): ServerResponse {
        val requestBody = req.awaitBody(RequestBody::class)
        val id = sampleRepository.insert(SampleEntity(
            UUID.randomUUID(),
            requestBody.type,
            requestBody.enabled,
            requestBody.number,
            requestBody.memo,
            Instant.now()
        )) ?: java.util.NoSuchElementException("error?")
        return ServerResponse.ok().bodyValueAndAwait(id)
    }

    suspend fun findById(req: ServerRequest): ServerResponse = req.pathVariable("id").let {
        ServerResponse.ok().bodyValueAndAwait(sampleRepository.findById(UUID.fromString(it)))
    }

    suspend fun findAll(req: ServerRequest) = ServerResponse.ok().bodyAndAwait(sampleRepository.findAll())
}