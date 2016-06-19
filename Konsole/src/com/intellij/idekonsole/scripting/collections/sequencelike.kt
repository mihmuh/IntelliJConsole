package com.intellij.idekonsole.scripting.collections

import com.intellij.idekonsole.scripting.collections.impl.SequenceSequenceLike
import java.util.*

interface SequenceLike<out T> {
    fun forEach(action: (T) -> Unit)
}

@Suppress("UNUSED")
fun <T> SequenceLike<T>.filter(predicate: (T) -> Boolean): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@filter.forEach { if (predicate(it)) action(it) }
    }
}

@Suppress("UNUSED")
fun <T : Any> SequenceLike<T?>.filterNotNull(): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@filterNotNull.forEach { if (it != null) action(it) }
    }
}

@Suppress("UNUSED")
inline fun <reified R> SequenceLike<*>.filterIsInstance(): SequenceLike<R> = object : SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@filterIsInstance.forEach { if (it is R) action(it) }
    }
}

@Suppress("UNUSED")
fun <R> SequenceLike<*>.filterIsInstance(klass: java.lang.Class<R>): SequenceLike<R> {
    @Suppress("UNCHECKED_CAST")
    return filter { klass.isInstance(it) } as SequenceLike<R>
}

@Suppress("UNUSED")
fun <T, R> SequenceLike<T>.map(transform: (T) -> R): SequenceLike<R> = object : SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@map.forEach { action(transform(it)) }
    }
}

@Suppress("UNUSED")
fun <T, R> SequenceLike<T>.flatMap(transform: (T) -> SequenceLike<R>): SequenceLike<R> = object : SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@flatMap.forEach { transform(it).forEach(action) }
    }
}

@Suppress("UNUSED")
fun <T> Sequence<T>.asSequenceLike(): SequenceLike<T> = SequenceSequenceLike(this)

@Suppress("UNUSED")
fun <T> SequenceLike<T>.toList(): List<T> {
    val result = ArrayList<T>()
    forEach() { result.add(it) }
    return result
}
