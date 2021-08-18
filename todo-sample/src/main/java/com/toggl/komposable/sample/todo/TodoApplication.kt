package com.toggl.komposable.sample.todo

import android.app.Application
import com.toggl.komposable.scope.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TodoApplication : Application(), CoroutineScope {
    @Inject
    lateinit var dispatchersProviders: DispatcherProvider

    override val coroutineContext: CoroutineContext by lazy {
        dispatchersProviders.main
    }
}
