package com.toggl.komposable.sample.petsnavigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel adapter turning the Store's cold flow into a hot StateFlow so UI survives
 * configuration changes without briefly showing the initial state again.
 */
class PetsViewModel : ViewModel() {
    private val store = petsStore

    private val _state = MutableStateFlow(PetsState())
    val state: StateFlow<PetsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.state.collect { latest ->
                _state.value = latest
            }
        }
    }
}
