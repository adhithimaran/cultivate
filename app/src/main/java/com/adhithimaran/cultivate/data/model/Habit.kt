package com.adhithimaran.cultivate.data.model

import com.google.firebase.Timestamp

enum class HabitType {
    DAILY, WEEKLY, MONTHLY
}

data class Habit (
    val id: String = "",
    val name: String = "",
    val durationMinutes: Int = 0,
    val type: HabitType = HabitType.DAILY,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any> = mapOf (
        "id" to id,
        "name" to name,
        "durationMinutes" to durationMinutes,
        "type" to type.name, // daily, weekly, or monthly
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>): Habit = Habit (
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            durationMinutes = (map["durationMinutes"] as? Long)?.toInt() ?: 0,
            type = HabitType.valueOf(map["type"] as? String ?: "DAILY"),
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}