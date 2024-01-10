package com.toggl.komposable.test.store

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

class JvmReflectionHandler : ReflectionHandler {
    override fun filterAccessibleProperty(
        properties: Collection<KProperty<*>>,
        predicate: (KProperty<*>) -> Boolean,
    ): List<KProperty<*>> {
        return properties.filter { property ->
            property.isAccessible = true
            predicate(property)
        }
    }

    override fun forEachAccessibleProperty(
        properties: Collection<KProperty<*>>,
        predicate: (KProperty<*>) -> Unit,
    ) {
        properties.forEach { property ->
            property.isAccessible = true
            predicate(property)
        }
    }
}
