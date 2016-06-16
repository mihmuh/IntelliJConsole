package com.intellij.idekonsole.scripting.collections

import com.intellij.idekonsole.KSettings
import java.util.*

fun <T> deepSearch(seed: T, f: (T) -> Sequence<T>): Sequence<T> {
    return sequenceOf(seed) + f(seed).flatMap { deepSearch(it, f) }
}

fun <T> wideSearch(seed: Sequence<T>, f: (T) -> Sequence<T>): Sequence<T> {
    return seed + wideSearch(seed.flatMap(f), f)
}

fun <T> Sequence<T>.isNotEmpty(): Boolean = iterator().hasNext()

fun <T> Sequence<T>.isEmpty(): Boolean = !isNotEmpty()

class IteratorSequence<out T>(private val iterator: Iterator<T>) : Sequence<T> by iterator.asSequence() {
    fun isEmpty(): Boolean = !iterator.hasNext()
}

fun <T, R> IteratorSequence<T>.map(transform: (T) -> R): IteratorSequence<R> =
        IteratorSequence(this.iterator().asSequence().map(transform).iterator())

fun <T> Sequence<T>.constrainOnce(): IteratorSequence<T> = IteratorSequence(iterator())

class HeadTailSequence<out T>(val evaluated: List<T>, val remaining: IteratorSequence<T>, val time: Long): Sequence<T> {
    var initialized = false
    override fun iterator(): Iterator<T> {
        if (initialized) throw IllegalStateException()
        initialized = true
        return (evaluated.asSequence().constrainOnce() + remaining).iterator()
    }
}

inline fun <reified R> HeadTailSequence<*>.castToType(): HeadTailSequence<R> {
    return HeadTailSequence<R>(evaluated.map { it as R }, remaining.map { it as R }, time)
}

fun <T, R> HeadTailSequence<T>.map(f: (T) -> R): HeadTailSequence<R> {
    return HeadTailSequence(evaluated.map { f(it) }, remaining.map { f(it) }, time)
}

fun <T> Sequence<T>.cacheHead(maxTime: Long = KSettings.TIME_LIMIT, maxSize: Int = 1000, minSize: Int = 1): HeadTailSequence<T> {
    val head = ArrayList<T>()
    val startTime = System.currentTimeMillis()
    val iterator = iterator()
    val finished = !iterator.hasNext()
    val timeExceeded = System.currentTimeMillis() - startTime > maxTime
    while (iterator.hasNext() && !finished && !(timeExceeded && head.size >= minSize) && !(head.size >= maxSize)) {
        head.add(iterator.next())
    }
    return HeadTailSequence(head, IteratorSequence(iterator), System.currentTimeMillis() - startTime)
}