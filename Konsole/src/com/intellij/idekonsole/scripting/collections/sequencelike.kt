package com.intellij.idekonsole.scripting.collections

import com.intellij.util.Processor
import java.util.*

interface SequenceLike<out T> {
    fun forEach(action: (T) -> Unit)
}

fun <T> sequenceLike(f: ((T) -> Unit) -> Unit): SequenceLike<T> = object: SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) = f(action)
}

fun <T> sequenceLikeProcessor(f: (Processor<T>) -> Unit): SequenceLike<T> = object: SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) = f.invoke(Processor { action(it); true })
}

fun <T> SequenceLike<T>.filter(predicate: (T) -> Boolean): SequenceLike<T> = object: SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@filter.forEach { if (predicate(it)) action(it) }
    }
}

fun <T: Any> SequenceLike<T?>.filterNotNull(): SequenceLike<T> = object: SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@filterNotNull.forEach { if (it != null) action(it) }
    }
}

inline fun <reified R> SequenceLike<*>.filterIsInstance(): SequenceLike<R> = object: SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@filterIsInstance.forEach { if (it is R) action(it) }
    }
}

fun <T, R> SequenceLike<T>.map(transform: (T) -> R): SequenceLike<R> = object: SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@map.forEach { action(transform(it)) }
    }
}

fun <T, R> SequenceLike<T>.flatMap(transform: (T) -> SequenceLike<R>): SequenceLike<R> = object: SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@flatMap.forEach { transform(it).forEach(action) }
    }
}

fun <T> Sequence<T>.asSequenceLike(): SequenceLike<T> = object: SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@asSequenceLike.forEach { action(it) }
    }
}

fun <T> SequenceLike<T>.toList(): List<T> {
    val result = ArrayList<T>()
    forEach { result.add(it) }
    return result
}
