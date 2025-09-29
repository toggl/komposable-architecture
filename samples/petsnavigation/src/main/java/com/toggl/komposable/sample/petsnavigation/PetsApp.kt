package com.toggl.komposable.sample.petsnavigation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.withoutEffect
import com.toggl.komposable.sample.petsnavigation.data.Animal
import com.toggl.komposable.sample.petsnavigation.data.sampleAnimals
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import com.toggl.komposable.utils.debugChanges
import com.toggl.komposable.utils.simplePrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

sealed class PetsAction {
    data object AnimalsTabClicked : PetsAction()
    data object SettingsTabClicked : PetsAction()
    data class AnimalClicked(val id: Int) : PetsAction()
    data object AboutClicked : PetsAction()
    data object BackEvent : PetsAction()
}

data class PetsState(
    val animals: List<Animal> = sampleAnimals,
    val backStack: List<NavKey> = listOf(AnimalListRoute),
) {
    val currentRoute = backStack.lastOrNull()
    val selectedAnimal: Animal? = animals.find { it.id == (currentRoute as? AnimalDetailRoute)?.id }
}

val petsAppReducer: Reducer<PetsState, PetsAction> = Reducer<PetsState, PetsAction> { state, action ->
    return@Reducer when (action) {
        PetsAction.AnimalsTabClicked ->
            state.copy(backStack = listOf(AnimalListRoute)).withoutEffect()
        PetsAction.SettingsTabClicked ->
            state.copy(backStack = listOf(AnimalListRoute, SettingsRoute)).withoutEffect()
        is PetsAction.AnimalClicked ->
            state.copy(backStack = state.backStack + AnimalDetailRoute(action.id)).withoutEffect()
        is PetsAction.AboutClicked ->
            state.copy(backStack = state.backStack + AboutRoute).withoutEffect()
        is PetsAction.BackEvent ->
            state.copy(backStack = state.backStack.dropLast(1)).withoutEffect()
    }
}.debugChanges(
    logger = { Log.d("debug", it) },
    printer = simplePrinter("PetsAppReducer"),
)

val dispatcherProvider = DispatcherProvider(
    io = Dispatchers.IO,
    computation = Dispatchers.Default,
    main = Dispatchers.Main,
)
val coroutineScope = object : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.main
}
val storeScopeProvider = StoreScopeProvider { coroutineScope }

val petsStore = createStore(
    initialState = PetsState(sampleAnimals, listOf(AnimalListRoute)),
    reducer = petsAppReducer,
    storeScopeProvider = storeScopeProvider,
    dispatcherProvider = dispatcherProvider,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetsNavigationApp() {
    val navigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState()
    val petsState by petsStore.state.collectAsState(initial = PetsState())

    LaunchedEffect(petsState.currentRoute) {
        if (petsState.currentRoute is TopLevelRoute) {
            navigationSuiteScaffoldState.show()
        } else {
            navigationSuiteScaffoldState.hide()
        }
    }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            listOf(
                AnimalListRoute,
                SettingsRoute,
            ).forEach { route ->
                item(
                    icon = {
                        Icon(
                            imageVector = route.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(route.label) },
                    selected = petsState.currentRoute == route,
                    onClick = { petsStore.send(route.clickAction) },
                )
            }
        },
        state = navigationSuiteScaffoldState,
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                NavDisplay(
                    backStack = petsState.backStack,
                    onBack = { petsStore.send(PetsAction.BackEvent) },
                    entryProvider = entryProvider {
                        entry<AnimalListRoute> {
                            AnimalListPage(
                                animals = petsState.animals,
                                onAnimalClick = { animalId ->
                                    petsStore.send(PetsAction.AnimalClicked(animalId))
                                },
                            )
                        }
                        entry<AnimalDetailRoute> {
                            AnimalDetailPage(petsState.selectedAnimal)
                        }
                        entry<SettingsRoute> {
                            SettingsPage(onAboutClick = {
                                petsStore.send(PetsAction.AboutClicked)
                            })
                        }
                        entry<AboutRoute> {
                            AboutPage()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutPage() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "About Page", fontSize = 36.sp)
    }
}

@Composable
private fun AnimalDetailPage(selectedAnimal: Animal?) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (selectedAnimal != null) {
            Text(text = selectedAnimal.name, fontSize = 36.sp)
            Text(text = selectedAnimal.description, fontSize = 12.sp)
        } else {
            Text(text = "Animal not found", fontSize = 36.sp)
        }
    }
}

@Composable
private fun AnimalListPage(animals: List<Animal>, onAnimalClick: (Int) -> Unit) {
    LazyColumn {
        animals.forEach { animal ->
            item {
                Column(
                    modifier = Modifier
                        .clickable { onAnimalClick(animal.id) }
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(animal.name, fontSize = 18.sp)
                    Text(animal.description, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(onAboutClick: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Settings", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAboutClick) {
            Text("About sub page")
        }
    }
}
