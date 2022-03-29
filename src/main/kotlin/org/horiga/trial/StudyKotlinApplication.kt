package org.horiga.trial

import kotlinx.coroutines.CoroutineExceptionHandler
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@SpringBootApplication
class StudyKotlinApplication

fun main(args: Array<String>) {
	runApplication<StudyKotlinApplication>(*args)
}

/**
 * refs: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-exception-handler/index.html
 */
class LoggingCoroutineExceptionHandler : CoroutineExceptionHandler,
										 AbstractCoroutineContextElement(CoroutineExceptionHandler) {

	companion object {
		val log = LoggerFactory.getLogger(LoggingCoroutineExceptionHandler::class.java)!!
	}

	override fun handleException(context: CoroutineContext, exception: Throwable) {
		log.error("UNCAUGHT EXCEPTION.", exception)
	}
}