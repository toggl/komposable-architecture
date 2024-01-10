package com.toggl.komposable.test.store

import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility

interface ReflectionHandler {
    fun filterAccessibleProperty(
        properties: Collection<KProperty<*>>,
        predicate: (KProperty<*>) -> Boolean,
    ): List<KProperty<*>>

    fun forEachAccessibleProperty(properties: Collection<KProperty<*>>, predicate: (KProperty<*>) -> Unit)
}

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
