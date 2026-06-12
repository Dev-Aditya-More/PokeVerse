package com.aditya1875.pokeverse.feature.inbox.data.repository

import com.aditya1875.pokeverse.feature.inbox.data.model.UserMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class InboxRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun inboxRef(uid: String) =
        firestore.collection("user_messages").document(uid).collection("inbox")

    suspend fun getMessages(): List<UserMessage> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = inboxRef(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            snapshot.documents.mapNotNull { it.toUserMessage() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun markAsRead(messageId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            inboxRef(uid).document(messageId).update("isRead", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val unread = inboxRef(uid).whereEqualTo("isRead", false).get().await()
            val batch = firestore.batch()
            unread.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendMessage(message: UserMessage) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val ref = inboxRef(uid).document()
            ref.set(
                mapOf(
                    "id" to ref.id,
                    "title" to message.title,
                    "body" to message.body,
                    "emoji" to message.emoji,
                    "type" to message.type,
                    "timestamp" to message.timestamp,
                    "isRead" to false
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun DocumentSnapshot.toUserMessage() = UserMessage(
        id = id,
        title = getString("title") ?: "",
        body = getString("body") ?: "",
        emoji = getString("emoji") ?: "🏆",
        type = getString("type") ?: "general",
        timestamp = getLong("timestamp") ?: 0L,
        isRead = getBoolean("isRead") ?: false
    )
}
