package com.aditya1875.pokeverse.feature.friends.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.friends.data.model.FriendProfile
import com.aditya1875.pokeverse.feature.friends.data.model.FriendRequest
import com.aditya1875.pokeverse.feature.friends.data.model.FriendStatus
import com.aditya1875.pokeverse.feature.friends.data.model.TrainerSearchResult
import com.aditya1875.pokeverse.feature.friends.data.repository.FriendsRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val repository: FriendsRepository,
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    val myProfile: StateFlow<UserProfile> =
        profileRepository.profileFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val incomingRequests: StateFlow<List<FriendRequest>> =
        repository.incomingRequestsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests: StateFlow<List<FriendRequest>> =
        repository.outgoingRequestsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val friendUids: StateFlow<List<String>> =
        repository.friendUidsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _friends = MutableStateFlow<List<FriendProfile>>(emptyList())
    val friends: StateFlow<List<FriendProfile>> = _friends.asStateFlow()

    private val _friendsLoading = MutableStateFlow(true)
    val friendsLoading: StateFlow<Boolean> = _friendsLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResultsRaw = MutableStateFlow<List<FriendProfile>>(emptyList())
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    // Search results annotated with the relationship to the current user
    val searchResults: StateFlow<List<TrainerSearchResult>> =
        combine(
            _searchResultsRaw, friendUids, outgoingRequests, incomingRequests
        ) { results, friends, outgoing, incoming ->
            val myUid = repository.currentUid
            results.map { profile ->
                val status = when {
                    profile.uid == myUid -> FriendStatus.SELF
                    friends.contains(profile.uid) -> FriendStatus.FRIENDS
                    outgoing.any { it.toUid == profile.uid } -> FriendStatus.REQUEST_SENT
                    incoming.any { it.fromUid == profile.uid } -> FriendStatus.REQUEST_RECEIVED
                    else -> FriendStatus.NONE
                }
                TrainerSearchResult(profile, status)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Refresh friend profiles whenever the friendship set changes
        viewModelScope.launch {
            friendUids.collect { uids ->
                _friendsLoading.value = true
                _friends.value = repository.getProfiles(uids)
                    .sortedByDescending { it.totalXp }
                _friendsLoading.value = false
            }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _searchQuery.debounce(350).collect { q ->
                if (q.trim().length < 2) {
                    _searchResultsRaw.value = emptyList()
                    return@collect
                }
                _searching.value = true
                _searchResultsRaw.value = repository.searchTrainers(q)
                _searching.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun sendRequest(to: FriendProfile) {
        viewModelScope.launch {
            val me = profileRepository.profileFlow.first()
            try {
                repository.sendRequest(
                    to = to,
                    fromName = me.username,
                    fromPhotoUrl = me.photoUrl,
                    fromLevel = me.level
                )
            } catch (_: Exception) {
            }
        }
    }

    fun cancelRequest(toUid: String) {
        viewModelScope.launch {
            try { repository.cancelRequest(toUid) } catch (_: Exception) { }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            try { repository.acceptRequest(request) } catch (_: Exception) { }
        }
    }

    fun declineRequest(request: FriendRequest) {
        viewModelScope.launch {
            try { repository.declineRequest(request) } catch (_: Exception) { }
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            try { repository.removeFriend(friendUid) } catch (_: Exception) { }
        }
    }
}
