package org.horiga.trial

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object Coroutine {

    val log = LoggerFactory.getLogger(Coroutine::class.java)!!

    data class DefaultThreadContext(
        val state: Map<String, String>
    ) {
        fun update(): AutoCloseable {
            // Update the local thread context
            log.info(">> execute thread context: update")

            return AutoCloseable {
                // Dispose
            }
        }

        companion object {
            val KEY = DefaultThreadContext::class.java
        }
    }

    class DefaultThreadContextElement : ThreadContextElement<AutoCloseable?>,
                                        AbstractCoroutineContextElement(Key) {
        override fun restoreThreadContext(context: CoroutineContext, oldState: AutoCloseable?) {
            log.info("[default-thread-context]: restore. ctx={}, oldState={}", context, oldState)
            oldState?.close()
        }

        override fun updateThreadContext(context: CoroutineContext): AutoCloseable? {
            log.info("[default-thread-context]: update. ctx={}", context)
            return context[ReactorContext]?.context?.let {
                it.getOrDefault<DefaultThreadContext>(DefaultThreadContext.KEY, null)?.update()
            }
        }

        companion object Key : CoroutineContext.Key<DefaultThreadContextElement>
    }
}

class R2dbcObservedCoroutineDispatcher(
    val delegate: CoroutineDispatcher
) : CoroutineDispatcher() {

    companion object {
        val log = LoggerFactory.getLogger(R2dbcObservedCoroutineDispatcher::class.java)!!
        val gaugeName = "kotlin_coroutine_dispatcher"
    }

    val gauge = Metrics.globalRegistry.gauge(gaugeName, Tags.of("observed", "r2dbc"), AtomicLong(0))!!

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context) {
            gauge.incrementAndGet()
            try {
                block.run()
            } finally {
                gauge.decrementAndGet()
            }
        }
    }
}

suspend inline fun <T> withR2dbcContext(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline block: suspend CoroutineScope.() -> T,
) = withContext(context + r2dbcContext(), block)

fun r2dbcContext() = MDCContext() + r2dbcCoroutineDispatcher + CoroutineName("r2dbc")

private val r2dbcCoroutineDispatcher = R2dbcObservedCoroutineDispatcher(Dispatchers.IO)