package com.adhithimaran.cultivate.data.repository

import androidx.compose.runtime.snapshotFlow
import com.adhithimaran.cultivate.data.model.Completion
import com.adhithimaran.cultivate.data.model.Habit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HabitRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Helper Func: Get curr user's base path OR throws error
    private fun userHabitsRef() = db
        .collection("users")
        .document(auth.currentUser!!.uid)
        .collection("habits")

    private fun userCompletionsRef() = db
        .collection("users")
        .document(auth.currentUser!!.uid)
        .collection("completions")

    // Adding new habit -> return: habit id
    suspend fun addHabit(habit: Habit): String {
        val docRef = userHabitsRef().document()
        val withId = habit.copy(id = docRef.id)
        docRef.set(withId.toMap()).await()
        return docRef.id
    }

    // stream of habits of curr user
    fun getHabitsFlow(): Flow<List<Habit>> = callbackFlow {
        val listener = userHabitsRef()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val habits = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Habit.fromMap(it) }
                } ?: emptyList()
                trySend(habits)
            }
        awaitClose { listener.remove() }
    }

    // del habit
    suspend fun deleteHabit(habitId: String) {
        userHabitsRef().document(habitId).delete().await()
    }

    // update habit
    suspend fun updateHabit(habit: Habit) {
        userHabitsRef().document(habit.id).set(habit.toMap()).await()
    }

    // Get all completions for specific habit
    fun getCompletionsForHabit(habitId: String): Flow<List<Completion>> = callbackFlow {
        val listener = userCompletionsRef()
            .whereEqualTo("habitId", habitId)
            .addSnapshotListener { snapshot,  error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val completions = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Completion.fromMap(it) }
                } ?: emptyList()
                trySend(completions)
            }
        awaitClose { listener.remove() }
    }

}