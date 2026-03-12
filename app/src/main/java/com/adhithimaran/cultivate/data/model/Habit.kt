package com.adhithimaran.cultivate.data.model

import com.google.firebase.Timestamp

enum class HabitType {
    DAILY, WEEKLY, MONTHLY
}

data class Habit(
    val id: String = "",
    val name: String = "",
    val durationMinutes: Int = 0,
    val type: HabitType = HabitType.DAILY,
    val createdAt: Timestamp = Timestamp.now()
) {
    /**
     * Converts this Habit into a plain Map so Firestore can serialize and store it.
     * Firestore cannot store Kotlin data classes directly — it only understands
     * primitive types, Strings, Timestamps, and Maps.
     *
     * Note: [HabitType] is stored as its String name (e.g. "DAILY") since
     * Firestore cannot store Kotlin enums directly.
     *
     * @return A [Map<String, Any>] representation of this Habit, ready to pass to Firestore.
     */
    fun toMap(): Map<String, Any> = mapOf(
        "id"              to id,
        "name"            to name,
        "durationMinutes" to durationMinutes,
        "type"            to type.name,
        "createdAt"       to createdAt
    )

    companion object {
        /**
         * Creates a [Habit] instance from a raw Firestore document map.
         * Used whenever data is read back from Firestore, which returns documents
         * as [Map<String, Any>] rather than typed Kotlin objects.
         *
         * Note: Firestore stores all integers as [Long] internally, so
         * [durationMinutes] must be received as Long and converted to Int.
         * The [type] field is stored as a String and converted back to a [HabitType] enum.
         *
         * @param map The raw document data returned by Firestore.
         * @return A fully typed [Habit] object, with safe fallback defaults for any missing fields.
         */
        fun fromMap(map: Map<String, Any>): Habit = Habit(
            id              = map["id"] as? String ?: "",
            name            = map["name"] as? String ?: "",
            durationMinutes = (map["durationMinutes"] as? Long)?.toInt() ?: 0,
            type            = HabitType.valueOf(map["type"] as? String ?: "DAILY"),
            createdAt       = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}