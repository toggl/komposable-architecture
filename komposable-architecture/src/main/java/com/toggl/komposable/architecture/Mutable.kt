package com.toggl.komposable.architecture
class Mutable<T>(private val getValue: () -> T, private val setValue: (T) -> Unit) {
    fun mutate(transformFn: T.() -> (T)) {
        val newValue = transformFn(getValue())
        setValue(newValue)
    }

    fun withValue(fn: T.() -> Unit) {
        fn(getValue())
    }

    fun <R> withValue(fn: T.() -> R): R =
        fn(getValue())

    internal fun <R> map(getMap: (T) -> R, mapSet: (T, R) -> T): Mutable<R> =
        Mutable(
            getValue = { getMap(getValue()) },
            setValue = { value -> this.mutate { mapSet(this, value) } }
        )
}
