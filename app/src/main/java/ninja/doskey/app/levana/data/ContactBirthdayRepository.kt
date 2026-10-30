package ninja.doskey.app.levana.data

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.BirthdayOccurrence
import ninja.doskey.app.levana.domain.model.ContactBirthday
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.GregorianCalendar

/**
 * Contact birthdays stored in the Contacts provider. Every provider call is a
 * main-safe suspend function running on [ioDispatcher].
 */
open class ContactBirthdayRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        const val MIME_TYPE =
            "vnd.android.cursor.item/com.levana.hebrew_birthday"
        private const val COL_DAY = ContactsContract.Data.DATA1
        private const val COL_MONTH = ContactsContract.Data.DATA2
        private const val COL_YEAR = ContactsContract.Data.DATA3
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
    }

    open suspend fun getAll(): List<ContactBirthday> {
        if (!hasContactsPermission()) return emptyList()

        val projection = arrayOf(
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            COL_DAY,
            COL_MONTH,
            COL_YEAR
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(MIME_TYPE)

        return context.contentResolver.queryCancellable(
            ioDispatcher,
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.Data.DISPLAY_NAME_PRIMARY} ASC"
        ) { cursor, signal ->
            val birthdays = mutableListOf<ContactBirthday>()
            val lookupIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.Data.LOOKUP_KEY
            )
            val nameIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.Data.DISPLAY_NAME_PRIMARY
            )
            val photoIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.Data.PHOTO_THUMBNAIL_URI
            )
            val dayIdx = cursor.getColumnIndexOrThrow(COL_DAY)
            val monthIdx = cursor.getColumnIndexOrThrow(COL_MONTH)
            val yearIdx = cursor.getColumnIndexOrThrow(COL_YEAR)

            while (cursor.moveToNext()) {
                signal.throwIfCanceled()
                val day = cursor.getString(dayIdx)?.toIntOrNull() ?: continue
                val month = cursor.getString(monthIdx)?.toIntOrNull()
                    ?: continue
                val year = cursor.getString(yearIdx)?.toIntOrNull() ?: 0
                birthdays.add(
                    ContactBirthday(
                        contactLookupKey = cursor.getString(lookupIdx) ?: "",
                        contactName = cursor.getString(nameIdx) ?: "",
                        contactPhotoUri = cursor.getString(photoIdx),
                        hebrewDay = day,
                        hebrewMonth = month,
                        hebrewYear = year
                    )
                )
            }
            birthdays
        } ?: emptyList()
    }

    // Birthdays never use the yahrzeit rule, so the yahrzeit settings do
    // not apply; the matching still goes through HebrewDateMatcher.occurrences.

    /** Birthdays observed on [date], each with its occurrence label (day panel, notifications). */
    suspend fun getBirthdaysForDate(date: LocalDate): List<BirthdayOccurrence> {
        val birthdays = getAll()
        val jd = jewishDate(date)
        return birthdays.mapNotNull { bday ->
            HebrewDateMatcher.occurrenceOn(
                bday.hebrewDay,
                bday.hebrewMonth,
                bday.hebrewYear,
                useYahrzeitRules = false,
                jd.jewishYear,
                jd.jewishMonth,
                jd.jewishDayOfMonth
            )?.let { BirthdayOccurrence(bday, it.label) }
        }
    }

    /** Birthdays observed from [start] to [end] (inclusive), by day, each with its occurrence label (agenda). */
    suspend fun getBirthdaysBetween(start: LocalDate, end: LocalDate): Map<LocalDate, List<BirthdayOccurrence>> {
        val birthdays = getAll()
        return withContext(ioDispatcher) {
            birthdays.flatMap { bday ->
                HebrewDateMatcher.occurrencesBetween(
                    bday.hebrewDay,
                    bday.hebrewMonth,
                    bday.hebrewYear,
                    useYahrzeitRules = false,
                    start,
                    end
                ).map { (date, occurrence) -> date to BirthdayOccurrence(bday, occurrence.label) }
            }.groupBy({ it.first }, { it.second })
        }
    }

    suspend fun getBirthdayDaysForHebrewMonth(year: Int, month: Int): Set<Int> {
        val birthdays = getAll()
        return withContext(ioDispatcher) {
            birthdays.flatMap { bday ->
                HebrewDateMatcher.occurrences(
                    bday.hebrewDay,
                    bday.hebrewMonth,
                    bday.hebrewYear,
                    useYahrzeitRules = false,
                    targetYear = year
                ).filter { it.month == month }.map { it.day }
            }.toSet()
        }
    }

    suspend fun getBirthdayDatesForGregorianMonth(year: Int, month: Int): Set<LocalDate> {
        val allBirthdays = getAll()
        if (allBirthdays.isEmpty()) return emptySet()
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()

        // A month of Hebrew-date matching per birthday: keep it off the caller
        return withContext(ioDispatcher) {
            (1..daysInMonth).map { LocalDate.of(year, month, it) }.filter { date ->
                val jd = jewishDate(date)
                allBirthdays.any { bday ->
                    HebrewDateMatcher.matchesDate(
                        bday.hebrewDay,
                        bday.hebrewMonth,
                        bday.hebrewYear,
                        useYahrzeitRules = false,
                        jd.jewishYear,
                        jd.jewishMonth,
                        jd.jewishDayOfMonth
                    )
                }
            }.toSet()
        }
    }

    private fun jewishDate(date: LocalDate) =
        JewishDate(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))

    open suspend fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) {
        val rawContactId = getRawContactId(contactLookupKey) ?: return

        // Check if a row already exists
        val existingDataId = getExistingDataId(rawContactId)

        val ops = ArrayList<ContentProviderOperation>()
        if (existingDataId != null) {
            // Update existing row
            ops.add(
                ContentProviderOperation.newUpdate(
                    ContactsContract.Data.CONTENT_URI
                )
                    .withSelection(
                        "${ContactsContract.Data._ID} = ?",
                        arrayOf(existingDataId.toString())
                    )
                    .withValue(COL_DAY, day.toString())
                    .withValue(COL_MONTH, month.toString())
                    .withValue(COL_YEAR, year.toString())
                    .build()
            )
        } else {
            // Insert new row
            ops.add(
                ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI
                )
                    .withValue(
                        ContactsContract.Data.RAW_CONTACT_ID,
                        rawContactId
                    )
                    .withValue(ContactsContract.Data.MIMETYPE, MIME_TYPE)
                    .withValue(COL_DAY, day.toString())
                    .withValue(COL_MONTH, month.toString())
                    .withValue(COL_YEAR, year.toString())
                    .build()
            )
        }

        applyBatch(ops)
    }

    open suspend fun removeBirthday(contactLookupKey: String) {
        val rawContactId = getRawContactId(contactLookupKey) ?: return
        val existingDataId = getExistingDataId(rawContactId) ?: return

        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newDelete(
                ContactsContract.Data.CONTENT_URI
            )
                .withSelection(
                    "${ContactsContract.Data._ID} = ?",
                    arrayOf(existingDataId.toString())
                )
                .build()
        )
        applyBatch(ops)
    }

    // applyBatch takes no CancellationSignal: it is skipped if cancellation
    // came first, otherwise a cancelled caller completes once it returns
    private suspend fun applyBatch(ops: ArrayList<ContentProviderOperation>) {
        runCancellable(ioDispatcher) { signal ->
            signal.throwIfCanceled()
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        }
    }

    open suspend fun getBirthdayForContact(contactLookupKey: String): ContactBirthday? {
        if (!hasContactsPermission()) return null

        val projection = arrayOf(
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            COL_DAY,
            COL_MONTH,
            COL_YEAR
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND " +
            "${ContactsContract.Data.LOOKUP_KEY} = ?"
        val selectionArgs = arrayOf(MIME_TYPE, contactLookupKey)

        return context.contentResolver.queryCancellable(
            ioDispatcher,
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs
        ) { cursor, _ ->
            if (!cursor.moveToFirst()) return@queryCancellable null
            val dayIdx = cursor.getColumnIndexOrThrow(COL_DAY)
            val monthIdx = cursor.getColumnIndexOrThrow(COL_MONTH)
            val yearIdx = cursor.getColumnIndexOrThrow(COL_YEAR)
            val nameIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.Data.DISPLAY_NAME_PRIMARY
            )
            val photoIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.Data.PHOTO_THUMBNAIL_URI
            )
            val day = cursor.getString(dayIdx)?.toIntOrNull()
                ?: return@queryCancellable null
            val month = cursor.getString(monthIdx)?.toIntOrNull()
                ?: return@queryCancellable null
            val year = cursor.getString(yearIdx)?.toIntOrNull() ?: 0
            ContactBirthday(
                contactLookupKey = contactLookupKey,
                contactName = cursor.getString(nameIdx) ?: "",
                contactPhotoUri = cursor.getString(photoIdx),
                hebrewDay = day,
                hebrewMonth = month,
                hebrewYear = year
            )
        }
    }

    /** The lookup key, display name and thumbnail of a contact returned by the picker. */
    open suspend fun resolveContact(contactUri: Uri): PickedContact? =
        context.contentResolver.queryCancellable(
            ioDispatcher,
            contactUri,
            arrayOf(
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            )
        ) { cursor, _ ->
            if (!cursor.moveToFirst()) return@queryCancellable null
            val lookupKey = cursor.getString(0) ?: return@queryCancellable null
            PickedContact(
                lookupKey = lookupKey,
                name = cursor.getString(1) ?: "Unknown",
                photoUri = cursor.getString(2)
            )
        }

    private suspend fun getRawContactId(lookupKey: String): Long? {
        val contactUri = runCancellable(ioDispatcher) {
            ContactsContract.Contacts.lookupContact(
                context.contentResolver,
                ContactsContract.Contacts.CONTENT_LOOKUP_URI
                    .buildUpon()
                    .appendPath(lookupKey)
                    .build()
            )
        } ?: return null

        val cId = firstLong(
            contactUri,
            ContactsContract.Contacts._ID,
            null,
            null
        ) ?: return null

        // Get raw contact ID
        return firstLong(
            ContactsContract.RawContacts.CONTENT_URI,
            ContactsContract.RawContacts._ID,
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(cId.toString())
        )
    }

    private suspend fun getExistingDataId(rawContactId: Long): Long? =
        firstLong(
            ContactsContract.Data.CONTENT_URI,
            ContactsContract.Data._ID,
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND " +
                "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), MIME_TYPE)
        )

    private suspend fun firstLong(
        uri: Uri,
        column: String,
        selection: String?,
        selectionArgs: Array<String>?
    ): Long? = context.contentResolver.queryCancellable(
        ioDispatcher,
        uri,
        arrayOf(column),
        selection,
        selectionArgs
    ) { cursor, _ ->
        if (cursor.moveToFirst()) cursor.getLong(0) else null
    }
}

data class PickedContact(
    val lookupKey: String,
    val name: String,
    val photoUri: String?
)
