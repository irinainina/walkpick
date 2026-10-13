package com.walkpick.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

val SyncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class Converters {

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun alternativesToText(value: List<FindAlternative>): String =
        SyncJson.encodeToString(ListSerializer(FindAlternative.serializer()), value)

    @TypeConverter
    fun textToAlternatives(value: String): List<FindAlternative> =
        SyncJson.decodeFromString(ListSerializer(FindAlternative.serializer()), value)
}

private fun text(name: String) = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

object InstantIso : KSerializer<Instant> {
    override val descriptor = text("InstantIso")
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    // Postgres answers with "+00:00", which Instant.parse refuses before API 31.
    override fun deserialize(decoder: Decoder): Instant =
        OffsetDateTime.parse(decoder.decodeString()).toInstant()
}

object LocalDateIso : KSerializer<LocalDate> {
    override val descriptor = text("LocalDateIso")
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

// A PostGIS line goes up as text and comes back as hex-encoded binary (EWKB).
object RouteWkb : KSerializer<String> {
    private const val SRID_FLAG = 0x20000000
    private const val LITTLE_ENDIAN: Byte = 1

    override val descriptor = text("RouteWkb")

    override fun serialize(encoder: Encoder, value: String) =
        encoder.encodeString("SRID=4326;LINESTRING($value)")

    override fun deserialize(decoder: Decoder): String {
        val bytes = decoder.decodeString().chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(if (buffer.get() == LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        if (buffer.int and SRID_FLAG != 0) buffer.int
        return List(buffer.int) { "${buffer.double} ${buffer.double}" }.joinToString(",")
    }
}
