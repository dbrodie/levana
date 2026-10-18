package ninja.doskey.app.levana.crash

import android.app.Application
import ninja.doskey.app.levana.R
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

/**
 * On a crash, asks the user whether to email a report to the developer.
 * Nothing leaves the device unless the user confirms and sends the email themselves.
 */
object CrashReporting {
    const val REPORT_EMAIL = "dbrodie@gmail.com"

    // No logcat or app data: they can contain event names or locations
    private val REPORT_FIELDS = listOf(
        ReportField.APP_VERSION_NAME,
        ReportField.APP_VERSION_CODE,
        ReportField.ANDROID_VERSION,
        ReportField.BRAND,
        ReportField.PHONE_MODEL,
        ReportField.PRODUCT,
        ReportField.USER_CRASH_DATE,
        ReportField.STACK_TRACE
    )

    /** Must be called from [Application.attachBaseContext] so crashes in onCreate are caught. */
    fun init(app: Application) {
        val versionName = app.packageManager
            .getPackageInfo(app.packageName, 0)
            .versionName
            .orEmpty()
        app.initAcra {
            reportFormat = StringFormat.KEY_VALUE_LIST
            reportContent = REPORT_FIELDS
            mailSender {
                mailTo = REPORT_EMAIL
                subject = app.getString(R.string.crash_report_subject, versionName)
                reportAsFile = false
            }
            dialog {
                title = app.getString(R.string.crash_dialog_title)
                text = app.getString(R.string.crash_dialog_text)
                positiveButtonText = app.getString(R.string.crash_dialog_send)
                negativeButtonText = app.getString(R.string.crash_dialog_dont_send)
                resTheme = R.style.Theme_Levana_CrashDialog
            }
        }
    }

    /** True in ACRA's separate `:acra` process, where the app's own startup should be skipped. */
    fun isReporterProcess(): Boolean = ACRA.isACRASenderServiceProcess()
}
