package com.toggl.komposable.test.utils

import com.toggl.komposable.test.store.ReflectionHandler
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

/**
 * A [ReflectionHandler] that filters and iterates over public properties of a class.
 * Works on JVM only.
 */
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
