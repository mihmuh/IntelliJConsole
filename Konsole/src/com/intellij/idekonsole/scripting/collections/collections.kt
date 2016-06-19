package com.intellij.idekonsole.scripting.collections

import com.intellij.idekonsole.KSettings
import java.util.*

fun <T> deepSearch(seed: T, f: (T) -> Sequence<T>): Sequence<T> {
    return sequenceOf(seed) + f(seed).flatMap { deepSearch(it, f) }
}

fun <T> wideSearch(seed: Sequence<T>, f: (T) -> Sequence<T>): Sequence<T> {
    return seed + wideSearch(seed.flatMap(f), f)
}

class IteratorSequence<out T>(private val iterator: Iterator<T>) : Sequence<T> by iterator.asSequence() {
    fun isEmpty(): Boolean = !iterator.hasNext()
    fun isNotEmpty(): Boolean = iterator.hasNext()
}

fun <T, R> IteratorSequence<T>.map(transform: (T) -> R): IteratorSequence<R> =
        IteratorSequence(this.iterator().asSequence().map(transform).iterator())

fun <T> Sequence<T>.constrainOnce(): IteratorSequence<T> = IteratorSequence(iterator())

class HeadTailSequence<out T>(val evaluated: List<T>, val remaining: IteratorSequence<T>, val time: Long, val options: CacheOptions) : Sequence<T> {
    var initialized = false
    override fun iterator(): Iterator<T> {
        if (initialized) throw IllegalStateException()
        initialized = true
        return (evaluated.asSequence().constrainOnce() + remaining).iterator()
    }
}

inline fun <reified R> HeadTailSequence<*>.castToType(): HeadTailSequence<R> {
    return HeadTailSequence(evaluated.map { it as R }, remaining.map { it as R }, time, options)
}

fun <T, R> HeadTailSequence<T>.map(f: (T) -> R): HeadTailSequence<R> {
    return HeadTailSequence(evaluated.map { f(it) }, remaining.map { f(it) }, time, options)
}

class CacheOptions(val minTime: Long = KSettings.SEARCH_TIME, val maxTime: Long = KSettings.TIME_LIMIT, val minSize: Int = 1, val maxSize: Int = KSettings.MAX_USAGES) {
    fun minTimeExceeded(timeElapsed: Long) = timeElapsed > minTime
    fun maxTimeExceeded(timeElapsed: Long) = timeElapsed > maxTime
    fun minSizeReached(size: Int) = size >= minSize
    fun maxSizeReached(size: Int) = size >= maxSize
}

fun <T> Sequence<T>.cacheHead(options: CacheOptions = CacheOptions()): HeadTailSequence<T> {
    val head = ArrayList<T>()
    val startTime = System.currentTimeMillis()
    val iterator = iterator()
    while (true) {
        val finished = !iterator.hasNext()
        val timeElapsed = System.currentTimeMillis() - startTime

        val minTimeExceeded = options.minTimeExceeded(timeElapsed)
        val maxTimeExceeded = options.maxTimeExceeded(timeElapsed)
        val minSizeReached = options.minSizeReached(head.size)
        val maxSizeReached = options.maxSizeReached(head.size)

        if (finished || minTimeExceeded && minSizeReached || maxTimeExceeded || maxSizeReached) {
            return HeadTailSequence(head, IteratorSequence(iterator), timeElapsed, options)
        }
        head.add(iterator.next())
    }
}