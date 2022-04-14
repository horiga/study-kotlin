package org.horiga.trial

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

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

class ObservedCoroutineDispatcher(
    val name: String,
    val delegate: CoroutineDispatcher
) : CoroutineDispatcher() {

    companion object {
        val log = LoggerFactory.getLogger(ObservedCoroutineDispatcher::class.java)!!
        val gaugeName = "kotlin_coroutine_dispatcher"
    }

    val gauge = Metrics.globalRegistry.gauge(gaugeName, Tags.of("name", name), AtomicLong(0))!!

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

// NOTE: R2DBCを使っているので、IO待ちするblocking taskではないので、本来 Dispatchers.IO を利用する必要はないはずだけど、検証のため使ってる
private val r2dbcCoroutineDispatcher = ObservedCoroutineDispatcher("r2dbc", Dispatchers.IO)

//---

class DefaultRequestContext(
) : ThreadContextElement<AutoCloseable>, AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<DefaultRequestContext>

    override fun restoreThreadContext(context: CoroutineContext, oldState: AutoCloseable) {
        oldState.close()
    }

    override fun updateThreadContext(context: CoroutineContext): AutoCloseable {
        return AutoCloseable {
            // todo
        }
    }
}

suspend inline fun <T> withDefaultContext(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline block: suspend CoroutineScope.() -> T
) = withContext(context + defaultContext(), block)

suspend inline fun defaultContext() =
    coroutineContext + MDCContext() + Dispatchers.Default + CoroutineName("default")
