package com.toggl.komposable.scope

import kotlinx.coroutines.CoroutineScope

fun interface StoreScopeProvider {
    fun getStoreScope(): CoroutineScope
}
