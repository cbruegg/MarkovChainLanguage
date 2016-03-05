package de.cbruegg.mcl

import java.util.*
import java.util.stream.Stream

/**
 * Transform the stream to a Kotlin sequence.
 */
fun <T> Stream<T>.asSequence(): Sequence<T> = iterator().asSequence()

/**
 * Merge a HashMap with another map by merging existing values with [f].
 */
fun <K, V> HashMap<K, V>.mergeWith(other: Map<K, V>, f: (V, V) -> V) {
    for ((k, v) in other) {
        merge(k, v, f)
    }
}

/**
 * Return a sequence of sequences where the inner sequence has a count of
 * up to [chunkSize].
 */
fun <T : Any> Sequence<T>.chunkify(chunkSize: Int): Sequence<Sequence<T>> = iterator().chunkify(chunkSize)

/**
 * Return a sequence of sequences where the inner sequence has a count of
 * up to [chunkSize].
 */
fun <T : Any> Iterator<T>.chunkify(chunkSize: Int): Sequence<Sequence<T>> =
        generateSequence outerSeq@ {
            var readElements = 0

            return@outerSeq if (hasNext()) generateSequence innerSeq@ {
                return@innerSeq if (hasNext() && readElements++ < chunkSize) {
                    next()
                } else null
            } else null
        }

/**
 * Return a random element from this list
 */
fun <E> List<E>.getRandom(): E = this[(Math.random() * size).toInt()]

/**
 * Return a random element from this list. O(n)
 */
fun <E> Set<E>.getRandom(): E {
    val entry = (Math.random() * size).toInt()
    var i = 0
    for (e in this) {
        if (i++ == entry) {
            return e
        }
    }
    throw ConcurrentModificationException("Size changed")
}



/**
 * Get notified about elements 'crossing' this sequence without modifying it.
 */
inline fun <T> Sequence<T>.peek(crossinline f: (T) -> Unit): Sequence<T> = map { f(it); it }