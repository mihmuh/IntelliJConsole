package com.intellij.idekonsole.scripting.collections

import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.context.runRead
import java.util.*

interface SequenceLike<out T> {
    fun forEach(action: (T) -> Unit)
}

fun <T> sequenceLike(f: ((T) -> Unit) -> Unit): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) = f(action)
}

fun <T> sequenceLikeProcessor(f: (com.intellij.util.Processor<T>) -> Unit): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) = f.invoke(com.intellij.util.Processor { action(it); true })
}

fun <T> SequenceLike<T>.filter(predicate: (T) -> Boolean): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@filter.forEach { if (predicate(it)) action(it) }
    }
}

fun <T : Any> SequenceLike<T?>.filterNotNull(): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) {
        this@filterNotNull.forEach { if (it != null) action(it) }
    }
}

inline fun <reified R> SequenceLike<*>.filterIsInstance(): SequenceLike<R> = object : SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@filterIsInstance.forEach { if (it is R) action(it) }
    }
}

fun <R> SequenceLike<*>.filterIsInstance(klass: java.lang.Class<R>): SequenceLike<R> {
    @Suppress("UNCHECKED_CAST")
    return filter { klass.isInstance(it) } as SequenceLike<R>
}

fun <T> SequenceLike<T>.wrapWithRead(): SequenceLike<T> {
    var context = Context.instance()
    return object : SequenceLike<T> {
        override fun forEach(action: (T) -> Unit) {
            this@wrapWithRead.forEach {
                runRead(context) {
                    action(it)
                }
            }
        }
    }
}

fun <T, R> SequenceLike<T>.map(transform: (T) -> R): SequenceLike<R> = object : SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@map.forEach { action(transform(it)) }
    }
}

fun <T, R> SequenceLike<T>.flatMap(transform: (T) -> SequenceLike<R>): SequenceLike<R> = object : SequenceLike<R> {
    override fun forEach(action: (R) -> Unit) {
        this@flatMap.forEach { transform(it).forEach(action) }
    }
}

class SequenceSequenceLike<out T>(val sequence: Sequence<T>) : SequenceLike<T> {
    var context = Context.instance()
    override fun forEach(action: (T) -> Unit) {
        val iterator = sequence.iterator()
        var hasNext: Boolean = true
        while (hasNext) {
            runRead(context) {
                if (iterator.hasNext()) {
                    action(iterator.next())
                } else {
                    hasNext = false
                }
            }
        }
    }
}

fun <T> Sequence<T>.asSequenceLike(): SequenceLike<T> = SequenceSequenceLike(this)

fun <T> SequenceLike<T>.toList(): List<T> {
    val result = ArrayList<T>()
    forEach() { result.add(it) }
    return result
}
