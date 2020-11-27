# Komposable Architecture
🏗️ Kotlin implementation of Point-Free's composable architecture

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

## Installation
Soon:tm:
