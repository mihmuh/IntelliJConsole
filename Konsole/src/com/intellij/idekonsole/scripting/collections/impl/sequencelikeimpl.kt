package com.intellij.idekonsole.scripting.collections.impl

import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.context.runRead
import com.intellij.idekonsole.scripting.collections.SequenceLike
import com.intellij.util.Processor

fun <T> sequenceLikeProcessor(f: (Processor<T>) -> Unit): SequenceLike<T> = object : SequenceLike<T> {
    override fun forEach(action: (T) -> Unit) = f.invoke(Processor { action(it); true })
}

class SequenceSequenceLike<out T>(val sequence: Sequence<T>) : SequenceLike<T> {
    var context = Context.instance()
    override fun forEach(action: (T) -> Unit) {
        val iterator = sequence.iterator()
        var hasNext: Boolean = true
        while (hasNext) {
            var next: T? = null
            runRead(context) {
                hasNext = iterator.hasNext()
                if (hasNext) {
                    next = iterator.next()
                }
            }
            if (hasNext) {
                action(next!!)
            }
        }
    }
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