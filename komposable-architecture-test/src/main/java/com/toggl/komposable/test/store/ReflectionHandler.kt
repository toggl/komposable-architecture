package com.toggl.komposable.test.store

import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility

/**
 * Utility interface for filtering and iterating over properties of a class.
 */
interface ReflectionHandler {
    fun filterAccessibleProperty(
        properties: Collection<KProperty<*>>,
        predicate: (KProperty<*>) -> Boolean,
    ): List<KProperty<*>>

    fun forEachAccessibleProperty(properties: Collection<KProperty<*>>, predicate: (KProperty<*>) -> Unit)
}

/**
 * A [ReflectionHandler] that filters and iterates over public properties of a class.
 */
class PublicPropertiesReflectionHandler : ReflectionHandler {
    override fun filterAccessibleProperty(
        properties: Collection<KProperty<*>>,
        predicate: (KProperty<*>) -> Boolean,
    ): List<KProperty<*>> = properties.filter { property ->
        property.visibility == KVisibility.PUBLIC && predicate(property)
    }

    override fun forEachAccessibleProperty(
        properties: Collection<KProperty<*>>,
        predicate: (KProperty<*>) -> Unit,
    ) = properties.forEach { property ->
        if (property.visibility == KVisibility.PUBLIC) {
            predicate(property)
        }
    }
}
