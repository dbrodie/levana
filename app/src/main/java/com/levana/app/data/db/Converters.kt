package com.levana.app.data.db

import androidx.room.TypeConverter
import com.levana.app.domain.model.EventType

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)
}
