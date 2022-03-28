package org.horiga.trial

import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext

class Test {

    companion object {
        fun p(s: String) {
            println("[${Thread.currentThread().name}]: $s")
        }
    }

    @org.junit.jupiter.api.Test
    fun `test for takeIf`() {
        val s: String? = "hello"

        listOf<String?>("Hello", "", "World", null, "END").map { s ->
            try {
                val result = s?.takeIf { it.isNotBlank() } ?: throw AssertionError("text is blank")
                println("result: $result")
            }catch (e: AssertionError) {
                println("assertion error: $s")
            }
        }
    }

    @org.junit.jupiter.api.Test
    fun `coroutines`() {

        // dispatcher
        val threadPool = Executors.newFixedThreadPool(10) { run -> Thread(null, run, "Me") }
        val dispatcher = threadPool.asCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

        scope.launch {

            p("@me")

            launch { p("@me(launch)") }

            launch(Dispatchers.IO) { p("@Dispatchers.IO") }

            launch(Dispatchers.Default) { p("@Dispatchers.Default") }

            withContext(Dispatchers.Default) { p("withContext@Dispatchers.Default") }
        }

        Thread.sleep(1000)
    }

    @org.junit.jupiter.api.Test
    fun `coroutine with CoroutineName`() {
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)

        val scope = CoroutineScope(CoroutineName("me"))
        scope.launch {
            p("1")
            launch { p("2") }
            launch(CoroutineName("sub-A")) { p("3") }
            launch(CoroutineName("sub-B")) { p("4") }
            val ctx = CoroutineName("sub-C")
            repeat(10) {
                launch(ctx) { p("N") }
            }
        }
        Thread.sleep(1000)
    }


}