package com.aditya1875.pokeverse.feature.inbox.data.repository

import android.content.Context
import com.aditya1875.pokeverse.feature.inbox.data.model.UserMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * The inbox: personal messages (user_messages/{uid}/inbox, written by Cloud Functions) plus
 * announcements, which the developer publishes to everyone from the Firebase console.
 *
 * An announcement is a document in `announcements/{id}`:
 *  - title, body, emoji (optional, default 📣), timestamp (ms since epoch, orders the inbox)
 *  - active (bool): false hides it without deleting it
 *  - popup (bool, optional): also show it once as a dialog on Home
 *  - minVersion / maxVersion (version codes, optional): who gets it. A "please update" note sets
 *    maxVersion to the current release, so only older builds see it
 *  - expiresAt (ms, optional): stops showing after this time
 * Whether the player has read it, or has already seen its popup, is kept on the device.
 */
class InboxRepository(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("announcements", Context.MODE_PRIVATE)

    private val versionCode: Long by lazy {
        runCatching {
            val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)
        }.getOrDefault(0L)
    }

    private var lastAnnouncements: List<UserMessage> = emptyList()
    private var popupIds: Set<String> = emptySet()

    private fun inboxRef(uid: String) =
        firestore.collection("user_messages").document(uid).collection("inbox")

    suspend fun getMessages(): List<UserMessage> {
        val personal = personalMessages()
        val announcements = announcements()
        return (personal + announcements).sortedByDescending { it.timestamp }.take(60)
    }

    private suspend fun personalMessages(): List<UserMessage> {
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

    private suspend fun announcements(): List<UserMessage> {
        return try {
            val now = System.currentTimeMillis()
            val read = prefs.getStringSet(KEY_READ, emptySet()).orEmpty()
            val popups = mutableSetOf<String>()
            val list = firestore.collection("announcements").whereEqualTo("active", true).get().await()
                .documents.mapNotNull { d ->
                    val min = d.getLong("minVersion") ?: 0L
                    val max = d.getLong("maxVersion") ?: Long.MAX_VALUE
                    val expires = d.getLong("expiresAt") ?: 0L
                    if (versionCode !in min..max || (expires in 1 until now)) return@mapNotNull null
                    val title = d.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val id = ID_PREFIX + d.id
                    if (d.getBoolean("popup") == true) popups += id
                    UserMessage(
                        id = id,
                        title = title,
                        body = d.getString("body").orEmpty(),
                        emoji = d.getString("emoji")?.takeIf { it.isNotBlank() } ?: "📣",
                        type = "announcement",
                        timestamp = d.getLong("timestamp") ?: 0L,
                        isRead = id in read,
                    )
                }
            lastAnnouncements = list
            popupIds = popups
            list
        } catch (e: Exception) {
            e.printStackTrace()
            lastAnnouncements
        }
    }

    /** The newest announcement flagged as a popup that this device has not shown yet, if any. */
    fun pendingPopup(): UserMessage? {
        val shown = prefs.getStringSet(KEY_POPUP_SHOWN, emptySet()).orEmpty()
        return lastAnnouncements.filter { it.id in popupIds && it.id !in shown }.maxByOrNull { it.timestamp }
    }

    fun markPopupShown(id: String) {
        val shown = prefs.getStringSet(KEY_POPUP_SHOWN, emptySet()).orEmpty()
        prefs.edit().putStringSet(KEY_POPUP_SHOWN, shown + id).apply()
    }

    suspend fun markAsRead(messageId: String) {
        if (messageId.startsWith(ID_PREFIX)) {
            val read = prefs.getStringSet(KEY_READ, emptySet()).orEmpty()
            prefs.edit().putStringSet(KEY_READ, read + messageId).apply()
            return
        }
        val uid = auth.currentUser?.uid ?: return
        try {
            inboxRef(uid).document(messageId).update("isRead", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead() {
        val read = prefs.getStringSet(KEY_READ, emptySet()).orEmpty()
        prefs.edit().putStringSet(KEY_READ, read + lastAnnouncements.map { it.id }).apply()
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

    private fun DocumentSnapshot.toUserMessage() = UserMessage(
        id = id,
        title = getString("title") ?: "",
        body = getString("body") ?: "",
        emoji = getString("emoji") ?: "🏆",
        type = getString("type") ?: "general",
        timestamp = getLong("timestamp") ?: 0L,
        isRead = getBoolean("isRead") ?: false
    )

    private companion object {
        const val ID_PREFIX = "ann_"
        const val KEY_READ = "read"
        const val KEY_POPUP_SHOWN = "popup_shown"
    }
}
