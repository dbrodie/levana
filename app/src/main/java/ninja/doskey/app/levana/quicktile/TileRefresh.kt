package ninja.doskey.app.levana.quicktile

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

/** Asks the system to rebind the Hebrew-date tile so it redraws the device's new date. */
fun requestHebrewDateTileRefresh(context: Context) {
    runCatching {
        TileService.requestListeningState(
            context,
            ComponentName(context, HebrewDateTileService::class.java)
        )
    }
}
