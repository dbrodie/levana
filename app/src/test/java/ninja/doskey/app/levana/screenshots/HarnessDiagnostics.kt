package ninja.doskey.app.levana.screenshots

import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.printToString
import org.robolectric.Shadows.shadowOf

/**
 * What a timed-out wait saw: every Compose root's semantics (dialogs are separate roots), the
 * window views, the main looper and the app's threads (Gradle's and the JVM's own are left
 * out), so one failure shows which of them stalled.
 */
internal object HarnessDiagnostics {

    fun describe(compose: ComposeTestRule): String = buildString {
        val looper = shadowOf(Looper.getMainLooper())
        appendLine("main looper: idle=${looper.isIdle} uptime=${SystemClock.uptimeMillis()} next task at ${looper.nextScheduledTaskTime}")
        appendLine("windows:")
        windowViews().forEach { view ->
            appendLine("  ${view.javaClass.name} attached=${view.isAttachedToWindow} ${view.width}x${view.height} visibility=${view.windowVisibility} layoutRequested=${view.isLayoutRequested}")
        }
        appendLine("compose roots:")
        appendLine(runCatching { compose.onAllNodes(isRoot(), useUnmergedTree = true).printToString(maxDepth = Int.MAX_VALUE) }.getOrElse { "  (failed: $it)" })
        appendLine("threads:")
        Thread.getAllStackTraces()
            .filter { (thread, stack) -> thread.isAlive && stack.none { it.className.startsWith("org.gradle.") } }
            .filterKeys { it.name !in JVM_THREADS }
            .entries.sortedBy { it.key.name }
            .forEach { (thread, stack) ->
                appendLine("  \"${thread.name}\" ${thread.state}")
                stack.take(STACK_DEPTH).forEach { appendLine("      at $it") }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun windowViews(): List<View> = runCatching {
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val field = global.getDeclaredField("mViews").apply { isAccessible = true }
        (field.get(instance) as List<View>).toList()
    }.getOrElse { emptyList() }

    private const val STACK_DEPTH = 14
    private val JVM_THREADS = setOf("Reference Handler", "Finalizer", "Signal Dispatcher", "Common-Cleaner", "Notification Thread")
}
