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
    val message = this.awaitBodyOrNull(clazz) ?: throw error ?: IllegalArgumentException("empty body")
    validator.validate(message).takeIf { it.isNotEmpty() }?.let { errors ->
        throw IllegalArgumentException(errors.joinToString {
            "'${it.propertyPath}' ${it.message}(rejected value '${it.invalidValue}'), "
        })
    }
    return message
}