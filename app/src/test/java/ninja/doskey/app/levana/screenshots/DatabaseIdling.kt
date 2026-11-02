package ninja.doskey.app.levana.screenshots

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Room's query and transaction executor for the harness's database, counting the queries in
 * flight so the harness's waits can include them. Compose's idling under Robolectric covers
 * composition and the main looper only (it does not consult registered idling resources), so
 * without this a wait could return while a screen's load was still on Room's thread, and the
 * test act on the screen before the load landed (TASK-60: the event editor's load closed a
 * picker opened before it).
 */
internal class DatabaseIdling : Executor {

    private val running = AtomicInteger()
    private val threads: ExecutorService = Executors.newCachedThreadPool { task ->
        Thread(task, "harness-room").apply { isDaemon = true }
    }

    val isIdle: Boolean get() = running.get() == 0

    override fun execute(command: Runnable) {
        running.incrementAndGet()
        try {
            threads.execute {
                try {
                    command.run()
                } finally {
                    running.decrementAndGet()
                }
            }
        } catch (e: RuntimeException) {
            running.decrementAndGet()
            throw e
        }
    }

    override fun toString() = "${running.get()} Room queries running"

    fun shutdown() {
        threads.shutdownNow()
    }
}
