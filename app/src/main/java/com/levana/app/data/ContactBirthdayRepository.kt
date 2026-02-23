package com.levana.app.data

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.levana.app.domain.model.ContactBirthday
import java.time.LocalDate
import java.util.GregorianCalendar

class ContactBirthdayRepository(private val context: Context) {

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

    fun getAll(): List<ContactBirthday> {
        if (!hasContactsPermission()) return emptyList()

        val birthdays = mutableListOf<ContactBirthday>()
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

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.Data.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
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
        }
        return birthdays
    }

    fun getBirthdaysForDate(date: LocalDate): List<ContactBirthday> {
        val gc = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        val jd = JewishDate(gc)
        val targetMonth = jd.jewishMonth
        val targetDay = jd.jewishDayOfMonth
        val targetIsLeapYear = jd.isJewishLeapYear

        return getAll().filter { bday ->
            HebrewDateMatcher.matchesDate(
                bday.hebrewDay,
                bday.hebrewMonth,
                bday.hebrewYear.coerceAtLeast(1),
                useYahrzeitRules = false,
                targetMonth,
                targetDay,
                targetIsLeapYear
            )
        }
    }

    fun getBirthdayDaysForHebrewMonth(year: Int, month: Int): Set<Int> {
        val isLeapYear = JewishDate(
            year,
            JewishDate.TISHREI,
            1
        ).isJewishLeapYear
        val days = mutableSetOf<Int>()

        for (bday in getAll()) {
            val eventIsLeapYear = if (bday.hebrewYear > 0) {
                JewishDate(
                    bday.hebrewYear,
                    JewishDate.TISHREI,
                    1
                ).isJewishLeapYear
            } else {
                false
            }
            val resolvedMonth = HebrewDateMatcher.resolveSimpleMonth(
                bday.hebrewMonth,
                eventIsLeapYear,
                isLeapYear
            )
            if (resolvedMonth == month) {
                days.add(bday.hebrewDay)
            }
        }
        return days
    }

    fun getBirthdayDatesForGregorianMonth(year: Int, month: Int): Set<LocalDate> {
        val allBirthdays = getAll()
        if (allBirthdays.isEmpty()) return emptySet()

        val dates = mutableSetOf<LocalDate>()
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()

        for (dayOfMonth in 1..daysInMonth) {
            val date = LocalDate.of(year, month, dayOfMonth)
            val gc = GregorianCalendar(year, month - 1, dayOfMonth)
            val jd = JewishDate(gc)
            val targetMonth = jd.jewishMonth
            val targetDay = jd.jewishDayOfMonth
            val targetIsLeapYear = jd.isJewishLeapYear

            if (allBirthdays.any { bday ->
                    HebrewDateMatcher.matchesDate(
                        bday.hebrewDay,
                        bday.hebrewMonth,
                        bday.hebrewYear.coerceAtLeast(1),
                        useYahrzeitRules = false,
                        targetMonth,
                        targetDay,
                        targetIsLeapYear
                    )
                }
            ) {
                dates.add(date)
            }
        }
        return dates
    }

    fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) {
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

        context.contentResolver.applyBatch(
            ContactsContract.AUTHORITY,
            ops
        )
    }

    fun removeBirthday(contactLookupKey: String) {
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
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    fun getBirthdayForContact(contactLookupKey: String): ContactBirthday? {
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

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
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
                    ?: return null
                val month = cursor.getString(monthIdx)?.toIntOrNull()
                    ?: return null
                val year = cursor.getString(yearIdx)?.toIntOrNull() ?: 0
                return ContactBirthday(
                    contactLookupKey = contactLookupKey,
                    contactName = cursor.getString(nameIdx) ?: "",
                    contactPhotoUri = cursor.getString(photoIdx),
                    hebrewDay = day,
                    hebrewMonth = month,
                    hebrewYear = year
                )
            }
        }
        return null
    }

    private fun getRawContactId(lookupKey: String): Long? {
        val contactUri = ContactsContract.Contacts.lookupContact(
            context.contentResolver,
            ContactsContract.Contacts.CONTENT_LOOKUP_URI
                .buildUpon()
                .appendPath(lookupKey)
                .build()
        ) ?: return null

        var contactId: Long? = null
        context.contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId = cursor.getLong(0)
            }
        }
        val cId = contactId ?: return null

        // Get raw contact ID
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(cId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun getExistingDataId(rawContactId: Long): Long? {
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND " +
                "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), MIME_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }
}
