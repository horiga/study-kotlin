package org.horiga.trial

import io.r2dbc.spi.Row
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import javax.validation.Validator
import kotlin.reflect.KClass

fun <T> Row.getOrThrow(name: String, klass: Class<T>): T {
    return this.get(name, klass) ?: throw NullPointerException("$name column must not NULL value.")
}

fun Row.stringOrThrow(name: String): String {
    return this.get(name, String::class.java) ?: throw NullPointerException("$name column must not NULL value.")
}

suspend fun <T : Any> ServerRequest.awaitBodyOrThrow(clazz: KClass<T>, error: Throwable? = null): T {
    return this.awaitBodyOrNull(clazz) ?: throw error ?: IllegalArgumentException("empty body")
}

// bodyToMono + validation
suspend fun <T : Any> ServerRequest.awaitBody(clazz: KClass<T>, validator: Validator, error: Throwable? = null): T {

    /*
      FIXME:
      type=UNKNOWN などの場合は bodyToMono で以下の Exception が発生して、GlobalExceptionHandler でハンドリングされず、
      ResponseStatusExceptionHandlerで、400 Bad Request が応答するんだけど、DEBUGログなのでエラーをハンドリングを統一できないのでどうにかしたい.

      o.s.w.s.h.ResponseStatusExceptionHandler [ResponseStatusExceptionHandler.java:handle:77] - [9098f25c-3] Resolved [ServerWebInputException: "400 BAD_REQUEST "Failed to read HTTP message"; nested exception is org.springframework.core.codec.DecodingException: JSON decoding error: Cannot deserialize value of type `org.horiga.trial.repository.SampleType` from String "A": not one of the values accepted for Enum class: [GENERAL, INDIVIDUAL, ADMIN]; nested exception is com.fasterxml.jackson.databind.exc.InvalidFormatException: Cannot deserialize value of type `org.horiga.trial.repository.SampleType` from String "A": not one of the values accepted for Enum class: [GENERAL, INDIVIDUAL, ADMIN]<EOL> at [Source: (io.netty.buffer.ByteBufInputStream); line: 1, column: 10] (through reference chain: org.horiga.trial.handler.SampleHandler$RequestBody["type"])"] for HTTP POST /samples

     */

    val message = this.awaitBodyOrNull(clazz) ?: throw error ?: IllegalArgumentException("empty body")
    validator.validate(message).takeIf { it.isNotEmpty() }?.let { errors ->
        throw IllegalArgumentException(errors.joinToString {
            "'${it.propertyPath}' ${it.message}(rejected value '${it.invalidValue}'), "
        })
    }
    return message
}