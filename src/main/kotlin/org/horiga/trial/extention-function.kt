package org.horiga.trial

import io.r2dbc.spi.Row
import org.springframework.web.reactive.function.server.ServerRequest

fun <T> Row.getOrThrow(name: String, klass: Class<T>): T {
    return this.get(name, klass) ?: throw NullPointerException("$name column must not NULL value.")
}

fun Row.stringOrThrow(name: String): String {
    return this.get(name, String::class.java) ?: throw NullPointerException("$name column must not NULL value.")
}
