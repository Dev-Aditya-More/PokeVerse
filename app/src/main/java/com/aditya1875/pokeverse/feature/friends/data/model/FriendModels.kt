package com.aditya1875.pokeverse.feature.friends.data.model

/**
 * Public trainer info projected from the `leaderboard` collection —
 * used for search results, friends list, and stat comparison.
 */
data class FriendProfile(
    val uid: String = "",
    val displayName: String = "Trainer",
    val photoUrl: String = "",
    val level: Int = 1,
    val totalXp: Int = 0,
    val weeklyXp: Int = 0
)

data class FriendRequest(
    val id: String = "",          // "{fromUid}_{toUid}"
    val fromUid: String = "",
    val fromName: String = "",
    val fromPhotoUrl: String = "",
    val fromLevel: Int = 1,
    val toUid: String = "",
    val toName: String = "",
    val toPhotoUrl: String = "",
    val createdAt: Long = 0L
)

/** Relationship of a searched trainer to the current user, drives the row's action button */
enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS, SELF }

data class TrainerSearchResult(
    val profile: FriendProfile,
    val status: FriendStatus
)
