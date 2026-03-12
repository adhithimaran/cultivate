package com.adhithimaran.cultivate.data.model

import com.google.firebase.Timestamp

data class Completion(
    val id: String = "",
    val habitId: String = "",
    val completedAt: Timestamp = Timestamp.now(),
    val type: HabitType = HabitType.DAILY
) {
    /**
     * Converts this Completion into a plain Map so Firestore can serialize and store it.
     * Firestore cannot store Kotlin data classes directly — it only understands
     * primitive types, Strings, Timestamps, and Maps.
     *
     * Note: [HabitType] is stored as its String name (e.g. "DAILY") since
     * Firestore cannot store Kotlin enums directly.
     *
     * @return A [Map<String, Any>] representation of this Completion, ready to pass to Firestore.
     */
    fun toMap(): Map<String, Any> = mapOf(
        "id"          to id,
        "habitId"     to habitId,
        "completedAt" to completedAt,
        "type"        to type.name
    )

    companion object {
        /**
         * Creates a [Completion] instance from a raw Firestore document map.
         * Used whenever data is read back from Firestore, which returns documents
         * as [Map<String, Any>] rather than typed Kotlin objects.
         *
         * Note: The [type] field is stored as a String in Firestore and converted
         * back to a [HabitType] enum here. Defaults to [HabitType.DAILY] if missing.
         *
         * @param map The raw document data returned by Firestore.
         * @return A fully typed [Completion] object, with safe fallback defaults for any missing fields.
         */
        fun fromMap(map: Map<String, Any>): Completion = Completion(
            id          = map["id"] as? String ?: "",
            habitId     = map["habitId"] as? String ?: "",
            completedAt = map["completedAt"] as? Timestamp ?: Timestamp.now(),
            type        = HabitType.valueOf(map["type"] as? String ?: "DAILY")
        )
    }
}