# Komposable Architecture [![Build Status](https://app.bitrise.io/app/8fc708d11fa0a5e5/status.svg?token=Q5m1YqGgX4VrIz4V2d0Olg&branch=main)](https://app.bitrise.io/app/8fc708d11fa0a5e5)
Kotlin implementation of Point-Free's composable architecture

## Motivations
When the time or rewritting Toggl's mobile apps came, we decided that we would take the native approach rather than insisting in using Xamarin
We quickly realized, however, that we could still share many things across the app, even if the apps didn't share a common codebase
The idea behind using the same architecture allowed us to share specs, github issues, creates a single common language that both Android and iOS devs can use and even speeds up development of features that are already implemented in the other platform!

We chose to use [Point-Free](https://www.pointfree.co/)'s Composable Architecture as the apps's architecture, which meant we had to set out to implement it in Kotlin. This repo is the result of our efforts!

## Differences from iOS

While all the core concepts are the same, the composable architecture is still written with Swift in mind, which means not everything can be translated 1:1 to Kotlin. Here are the problems we faced and the solutions we found:

### No KeyPaths
The lack of KeyPaths in Kotlin forces us to use functions in order to map from global state to local state.

### No Value Types
There's no way to simply mutate the state in Kotlin like the Composable architecture does in Swift. The fix for this is the [`Mutable`](https://github.com/toggl/komposable-architecture/blob/main/komposable-architecture/src/main/java/com/toggl/komposable/architecture/Mutable.kt) class which allows for the state to be mutate without requiring the user to explicitly returning the new copy of the state, making the Reducers read a lot more like their iOS counterparts.

### Subscriptions
Additionally we decided to extend Point-Free architecture with something we call subscriptions. This concept is taken from the [Elm Architecture](https://guide.elm-lang.org/architecture/). It's basically a way for us to leverage observable capabilities of different APIs, in our case it's mostly for observing data stored in [Room Database](https://developer.android.com/training/data-storage/room).

### Sample code
- [Todo Sample](https://github.com/toggl/komposable-architecture/tree/main/todo-sample)

## Installation
Soon:tm:

## Parts of The Architecture

This is a high level overview of the different parts of the architecture. 

- **Views** This is anything that can subscribe to the store to be notified of state changes. Normally this happens only in UI elements, but other elements of the app could also react to state changes.
- **Action** Simple structs that describe an event, normally originated by the user, but also from other sources or in response to other actions (from Effects). The only way to change the state is through actions. Views dispatch actions to the store which handles them in the main thread as they come.
- **Store** The central hub of the application. Contains the whole state of the app, handles the dispatched actions passing them to the reducers and fires Effects.
- **State** The single source of truth for the whole app. This data class will be probably empty when the application start and will be filled after every action. 
- **Reducers** Reducers are pure functions that take the state and an action and produce a new state. Simple as that. They optionally result in an array of Effects that will asynchronously dispatch further actions. All business logic should reside in them.
- **Effects** As mentioned, Reducers optionally produce these after handling an action. They are classes that return an optional effect. All the effects emitted from a reducer will be batched, meaning the state change will only be emitted once all actions are dispatched.
- **Subscriptions** Subscriptions are emitting actions based on some underling observable API and/or state changes.   

There's one global `Store` and one `AppState`. But we can *view* into the store to get sub-stores that only work on one part of the state. More on that later.

There's also one main `Reducer` but multiple sub-reducers that handle a limited set of actions and only a part of the state. Those reducers are then *pulled back* and *combined* into the main 
reducer.

## Store & State

The `Store` exposes a flow which emits the whole state of the app every time there's a change and a method to dispatch actions that will modify that state.  The `State` is just a data class that contains ALL the state of the application. It also includes the local state of all the specific modules that need local state. More on this later.

The store interface looks like this:

```kotlin
interface Store<State, Action : Any> {
    val state: Flow<State>
    fun dispatch(actions: List<Action>)
    // more code
}
```

And you can create a new store using:

```kotlin
createStore(
    initialState = AppState(),
    reducer = reducer,
    subscription = subscription,
    dispatcherProvider = dispatcherProvider,
    storeScopeProvider = application as StoreScopeProvider
)
```

actions are dispatched like this:

```kotlin
store.dispatch(AppAction.BackPressed)
```

and views can subscribe like this:

```kotlin
store.state
    .onEach { Log.d(tag, "The whole state: \($0)") }
    .launchIn(scope)

// or

store.state
    .map { it.email }
    .onEach { emailTextField.text = it }
    .launchIn(scope)
```

The store can be "viewed into", which means that we'll treat a generic store as if it was a more specific one which deals with only part of the app state and a subset of the actions. More on the Store Views section.

## Actions

Actions are sealed classes, which makes it easier to discover which actions are available and also add the certainty that we are handling all of them in reducers.

```kotlin
sealed class EditAction {
    data class TitleChanged(val title: String) : EditAction()
    data class DescriptionChanged(val description: String) : EditAction()
    object CloseTapped : EditAction()
    object SaveTapped : EditAction()
    object Saved : EditAction()
}
```

These sealed actions are embedded into each other starting with the "root" `AppAction`

```kotlin
sealed class AppAction {
    class List(override val action: ListAction) : AppAction(), ActionWrapper<ListAction>
    class Edit(override val action: EditAction) : AppAction(), ActionWrapper<EditAction>
    object BackPressed : AppAction()
}
```

So to dispatch an `EditAction` to a store that takes `AppActions` we would do

```kotlin
store.dispatch(AppAction.Edit(EditAction.TitleChanged("new title"))
```

But if the store is a view that takes `EditAction`s we'd do it like this:

```kotlin
store.dispatch(EditAction.TitleChanged("new title"))
```

## Reducers & Effects

Reducers are classes that implement the following interface:

```kotlin
interface Reducer<State, Action> {
    fun reduce(state: MutableValue<State>, action: Action): List<Effect<Action>>
}
```

The idea is they take the state and an action and modify the state depending on the action and its payload.

In order to dispatch actions asynchronously we use `Effect`s. Reducers return an array of `Effect`s. The store waits for those effects and dispatches whatever action they emit, if any.

An effect interface is also straightforward:

```kotlin
interface Effect<out Action> {
    suspend fun execute(): Action?
}
```

## Subscriptions
Subscriptions are similar to effects:

```kotlin
fun interface Subscription<State, Action : Any> {
    fun subscribe(state: Flow<State>): Flow<Action>
}
```

The difference is that Subscriptions are not triggered by Actions. They start immediately after the store is created and continue emitting as long as the store exists. 

Subscriptions are typically used to observe some data in the database:

```kotlin
class ListSubscription @Inject constructor(val todoDao: TodoDao) : Subscription<AppState, AppAction> {
    override fun subscribe(state: Flow<AppState>): Flow<AppAction> =
        todoDao.getAll().map { AppAction.List(ListAction.ListUpdated(it)) }
}
```

Or some other observable API like for example location services. Subscription flow can be also steered by state changes:

```kotlin
class ListSubscription @Inject constructor(val locationProvider: LocationProvider) : Subscription<AppState, AppAction> {
    override fun subscribe(state: Flow<AppState>): Flow<AppAction> =
        if (state.isPermissionGranted) 
          locationProvider.observeCurrentLocation().map { AppAction.Map(MapAction.LocationUpdated(it)) }
        else 
          flowOf()
}
```


## Pullback

There's one app level reducer that gets injected into the store. This reducer takes the whole `AppState` and the complete set of `AppActions`. 

The rest of the reducers only handle one part of that state, for a particular subset of the actions.

This aids in modularity. But in order to merge those reducers with the app level one, their types need to be compatible. That's what `pullback` is for. It converts a specific reducer into a global one.

```kotlin
class PullbackReducer<LocalState, GlobalState, LocalAction, GlobalAction>(
    private val innerReducer: Reducer<LocalState, LocalAction>,
    private val mapToLocalState: (GlobalState) -> LocalState,
    private val mapToLocalAction: (GlobalAction) -> LocalAction?,
    private val mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    private val mapToGlobalAction: (LocalAction) -> GlobalAction
) : Reducer<GlobalState, GlobalAction> {
    override fun reduce(
        state: Mutable<GlobalState>,
        action: GlobalAction
    ): List<Effect<GlobalAction>> {
        val localAction = mapToLocalAction(action)
            ?: return noEffect()

        return innerReducer
            .reduce(state.map(mapToLocalState, mapToGlobalState), localAction)
            .map { effect -> effect.map { action -> action?.run(mapToGlobalAction) } }
    }
}
```

After we've transformed the reducer we can use `combine` to merge it with other reducers to create one single reducer that is then injected into the store.

## Store Views

Similarly to reducers and pullback, the store itself can be "mapped" into a specific type of store that only holds some part of the state and only handles some subset of actions. Only this operation is not exactly "map", so it's called `view`.

```kotlin
class MutableStateFlowStore<State, Action : Any> private constructor(
    override val state: Flow<State>,
    private val dispatchFn: (List<Action>) -> Unit
) : Store<State, Action> {

    override fun <ViewState, ViewAction : Any> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
        state = state.map { mapToLocalState(it) }.distinctUntilChanged(),
        dispatchFn = { actions ->
            val globalActions = actions.mapNotNull(mapToGlobalAction)
            dispatchFn(globalActions)
        }
    )
}
```

This method on `Store` takes two closures, one to map the global state into local state and another one to the opposite for the actions.

Different modules or features of the app use different store views so they are only able to listen to changes to parts of the state and are only able to dispatch certain actions.

## Local State

Some features have the need of adding some state to be handled by their reducer, but maybe that state is not necessary for the rest of the application. Consider email & password fields in a theoretical Auth module.

To deal with this kind of state we do the following:
- In the module's state use a public class with internal properties to store the needed local state
- We store that property in the global state. So that state in the end is part of the global state and it behaves the same way, but can only be accessed from the module that needs it.

This is how could the AuthState look like:
```kotlin
data class AuthState(
    val user: Loadable<User>,
    val localState: LocalState
) {
    data class LocalState internal constructor(
        internal val email: Email,
        internal val password: Password
    ) {
        constructor() : this(Email.Invalid(""), Password.Invalid(""))
    }
}
```

This is how it looks in the global app state
```kotlin
data class AppState(
    val authLocalState: AuthState.LocalState = AuthState.LocalState(),
)
```

## High-order reducers

High-order reducers are basically reducers that take another reducer (and maybe also some other parameters). The outer reducer adds some behavior to the inner one, maybe transforming actions, stopping them or doing something with them before sending them forward to the inner reducer.

The simplest example of this is a logging reducer, which logs every dispatched action to the console:


```kotlin
class LoggingReducer(override val innerReducer: Reducer<AppState, AppAction>)
    : HigherOrderReducer<AppState, AppAction> {
    override fun reduce(
        state: MutableValue<AppState>,
        action: AppAction
    ): List<Effect<AppAction>> {
        Log.i(
            "LoggingReducer", when (action) {
                is AppAction.List -> action.list.formatForDebug()
                is AppAction.Edit -> action.edit.formatForDebug()
            }
        )

        return innerReducer.reduce(state, action)
    }
}
```
