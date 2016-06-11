package com.intellij.idekonsole.scripting

import java.util.*

fun <T> deepSearch(seed: T, f: (T) -> Sequence<T>): Sequence<T> {
    return sequenceOf(seed) + f(seed).flatMap { deepSearch(it, f) }
}

fun <T> wideSearch(seed: Sequence<T>, f: (T) -> Sequence<T>): Sequence<T> {
    return seed + wideSearch(seed.flatMap(f), f)
}

fun <T> Sequence<T>.isNotEmpty(): Boolean = iterator().hasNext()

fun <T> Sequence<T>.isEmpty(): Boolean = !isNotEmpty()

class IteratorSequence<out T>(private val iterator: Iterator<T>): Sequence<T> by iterator.asSequence() {
    fun isEmpty(): Boolean = !iterator.hasNext()
}

fun <T> Sequence<T>.constrainOnce(): IteratorSequence<T> = IteratorSequence(iterator())

class PartiallyEvaluatedSequence<out T>(val evaluated: List<T>, val remaining: IteratorSequence<T>): Sequence<T> {
    override fun iterator(): Iterator<T> {
        return evaluated.asSequence().plus(remaining).iterator()
    }
}

fun <T> Sequence<T>.evaluate(time: Int): PartiallyEvaluatedSequence<T> {
    val head = ArrayList<T>()
    val startTime = System.currentTimeMillis()
    val iterator = iterator()
    while (iterator.hasNext() && System.currentTimeMillis() - startTime < time) {
        head.add(iterator.next())
    }
    return PartiallyEvaluatedSequence(head, IteratorSequence(iterator))
}
