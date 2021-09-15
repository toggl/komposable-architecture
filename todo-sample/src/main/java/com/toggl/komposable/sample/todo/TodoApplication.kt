package com.toggl.komposable.sample.todo

import android.app.Application
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltAndroidApp
class TodoApplication : Application(), CoroutineScope, StoreScopeProvider {
    @Inject
    lateinit var dispatchersProviders: DispatcherProvider

    override val coroutineContext: CoroutineContext by lazy {
        dispatchersProviders.main
    }

    override fun getStoreScope(): CoroutineScope =
        this
}
