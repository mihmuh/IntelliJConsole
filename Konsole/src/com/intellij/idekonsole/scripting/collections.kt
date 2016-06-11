package com.intellij.idekonsole.scripting

fun <T> deepSearch(seed: T, f: (T) -> Sequence<T>): Sequence<T> {
    return sequenceOf(seed) + f(seed).flatMap { deepSearch(it, f) }
}

fun <T> wideSearch(seed: Sequence<T>, f: (T) -> Sequence<T>): Sequence<T> {
    return seed + wideSearch(seed.flatMap(f), f)
}

fun <T> Sequence<T>.isNotEmpty(): Boolean = iterator().hasNext()

fun <T> Sequence<T>.isEmpty(): Boolean = !isNotEmpty()