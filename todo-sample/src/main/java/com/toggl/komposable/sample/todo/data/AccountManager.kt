package com.toggl.komposable.sample.todo.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor() {
    fun getLoggedInUser(): Flow<Identity> = flow {
        emit(Identity.Unknown)
        delay(3000)
        emit(Identity.User("John Balance", "john.balance@coil.com"))
    }
}

sealed class Identity {
    object Unknown : Identity()
    data class User(val username: String, val email: String) : Identity()
}
