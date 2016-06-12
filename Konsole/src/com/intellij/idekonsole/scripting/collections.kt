package com.intellij.idekonsole.scripting

import com.intellij.idekonsole.KSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.openapi.project.Project
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

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

fun <T> Sequence<T>.constrainOnce(): IteratorSequence<T> = IteratorSequence(iterator())

class PartiallyEvaluatedSequence<out T>(val evaluated: List<T>, val remaining: IteratorSequence<T>, val time: Long, val originalSequence: Sequence<T>)

class CachedHeadSequence<out T>(val evaluated: List<T>, val hasRemaining: Boolean, val time: Long, val originalSequence: Sequence<T>) : Sequence<T> {
    override fun iterator(): Iterator<T> = originalSequence.iterator()
}

inline fun <reified R> CachedHeadSequence<*>.castToType(): CachedHeadSequence<R> {
    return CachedHeadSequence<R>(evaluated.map { it as R }, hasRemaining, time, originalSequence.map { it as R })
}

fun <T, R> CachedHeadSequence<T>.map(f: (T) -> R): CachedHeadSequence<R> {
    return CachedHeadSequence(evaluated.map { f(it) }, hasRemaining, time, originalSequence.map { f(it) })
}

fun <T : Any> CachedHeadSequence<T?>.filterNotNull(): CachedHeadSequence<T> {
    return CachedHeadSequence(evaluated.filterNotNull(), hasRemaining, time, originalSequence.filterNotNull())
}

fun <T> Sequence<T>.cacheHead(maxTime: Long = KSettings.TIME_LIMIT, maxSize: Int = 1000, minSize: Int = 1): CachedHeadSequence<T> {
    if (this is CachedHeadSequence) {
        return this
    }
    val partiallyEvaluatedSequence = evaluateInternal(maxTime, maxSize, minSize)
    if (this is LazyCancelableSequence<*>) {
        cancel()
    }
    return CachedHeadSequence(partiallyEvaluatedSequence.evaluated, !partiallyEvaluatedSequence.remaining.isEmpty(), partiallyEvaluatedSequence.time, partiallyEvaluatedSequence.originalSequence)
}

class HeadTailSequence<out T>(val evaluated: List<T>, val remaining: IteratorSequence<T>, val time: Long)

fun <T> Sequence<T>.evaluateHead(maxTime: Long, maxSize: Int = 1000, minSize: Int = 1): HeadTailSequence<T> {
    val partiallyEvaluatedSequence = evaluateInternal(maxTime, maxSize, minSize)
    return HeadTailSequence(partiallyEvaluatedSequence.evaluated, partiallyEvaluatedSequence.remaining, partiallyEvaluatedSequence.time)
}

private fun <T> Sequence<T>.evaluateInternal(maxTime: Long, maxSize: Int = 1000, minSize: Int = 1): PartiallyEvaluatedSequence<T> {
    val head = ArrayList<T>()
    val startTime = System.currentTimeMillis()
    val iterator = iterator()
    val finished = !iterator.hasNext()
    val timeExceeded = System.currentTimeMillis() - startTime > maxTime
    while (!finished && !(timeExceeded && head.size >= minSize) && !(head.size >= maxSize)) {
        head.add(iterator.next())
    }
    return PartiallyEvaluatedSequence(head, IteratorSequence(iterator), System.currentTimeMillis() - startTime, this)
}

interface LazyCancelableSequence<out T> : Sequence<T> {
    fun cancel()
}

private class ProcessorSequence<out T>(project: Project, handler: (Processor<T>) -> Unit) : LazyCancelableSequence<T> {
    private val buffer = ContainerUtil.createConcurrentList<T>()
    val lock = Object()
    var finished: Boolean = false
    val waiting = AtomicInteger(0)
    var cancelled = false

    init {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Searching") {
            override fun run(indicator: ProgressIndicator) {
                handler.invoke(Processor<T> {
                    buffer.add(it)
                    if (waiting.get() > 0) {
                        synchronized(lock) {
                            if (waiting.get() > 0) {
                                lock.notifyAll()
                            }
                        }
                    }
                    true
                })
                finished = true
                if (waiting.get() > 0) {
                    synchronized(lock) {
                        if (waiting.get() > 0) {
                            lock.notifyAll()
                        }
                    }
                }
            }
        })
    }

    override fun cancel() {
        val indicator = ProgressWrapper.unwrap(ProgressManager.getInstance().progressIndicator)
        indicator.cancel()
        cancelled = true
    }

    override fun iterator(): Iterator<T> {
        var nextIndex: Int = 0
        return object : Iterator<T> {
            override fun hasNext(): Boolean {
                assert(!cancelled)
                assert(nextIndex <= buffer.size)
                if (nextIndex < buffer.size) {
                    return true
                } else if (finished) {
                    return false
                }
                synchronized(lock) {
                    waiting.incrementAndGet()
                    while (nextIndex == buffer.size && !finished) {
                        lock.wait()
                    }
                    waiting.decrementAndGet()
                }
                return nextIndex < buffer.size
            }

            override fun next(): T {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                return buffer[nextIndex++]
            }
        }
    }
}

fun <T> concurrentPipe(project: Project, handler: (Processor<T>) -> Unit): LazyCancelableSequence<T> {
    return ProcessorSequence(project, handler)
}

//non concurrent version
fun <T> pipeByList(handler: (Processor<T>) -> Unit): Sequence<T> {
    val buffer = ArrayList<T>()
    handler(Processor {
        buffer.add(it)
    })
    return buffer.asSequence()
}
