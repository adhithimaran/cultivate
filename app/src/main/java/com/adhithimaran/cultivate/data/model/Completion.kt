package com.adhithimaran.cultivate.data.model

import com.google.firebase.Timestamp

data class Completion(
    val id: String = "",
    val habitId: String = "",
    val completedAt: Timestamp = Timestamp.now(),
    val type: HabitType = HabitType.DAILY
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "habitId" to habitId,
        "completedAt" to completedAt,
        "type" to type.name
    )

    companion object {
        fun fromMap(map: Map<String, Any>): Completion = Completion(
            id = map["id"] as? String ?: "",
            habitId = map["habitId"] as? String ?: "",
            completedAt = map["completedAt"] as? Timestamp ?: Timestamp.now(),
            type = HabitType.valueOf(map["type"] as? String ?: "DAILY")
        )
    }
}