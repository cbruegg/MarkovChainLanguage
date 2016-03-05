package de.cbruegg.mcl

import de.cbruegg.mcl.proto.MarkovChainOuterClass.MarkovChain
import de.cbruegg.mcl.proto.MarkovChainOuterClass.TransitionsFrom
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * A representation of a word following another in the corpus.
 */
data class Bigram(val first: String, val second: String)

/**
 * Returns a sequence of word pairs as seen in the text.
 */
fun Sequence<String>.toBigrams(): Sequence<Bigram> {
    val iter = iterator()
    if (!iter.hasNext()) {
        return emptySequence()
    }

    var last = iter.next()
    return generateSequence {
        if (iter.hasNext()) {
            val current = iter.next()
            val bigram = Bigram(last, current)
            last = current
            return@generateSequence bigram
        }

        return@generateSequence null
    }
}

/**
 * Computes a map of words to their successors, including the number of occurrences of
 * a pair of them.
 */
fun Sequence<Bigram>.wordFollowCountsByPrevious(): HashMap<String, HashMap<String, Int>> {
    val result = HashMap<String, HashMap<String, Int>>()
    for (bigram in this) {
        val followCounts = result.computeIfAbsent(bigram.first) { HashMap() }
        followCounts[bigram.second] = followCounts.getOrDefault(bigram.second, 0) + 1
    }
    return result
}

/**
 * Convert a map of words to their successors and the respective number of occurrences of a pair of them
 * to a markov chain reflecting their relative frequencies as transitions.
 */
fun Map<String, Map<String, Int>>.wordFollowCountsByPreviousToChain(): MarkovChain {
    val markovBuilder = MarkovChain.newBuilder()

    // Status vars
    var runs = 0
    var lastPercentage = -1

    for ((firstWord, followCounts) in this) {
        val transitionsFromBuilder = TransitionsFrom.newBuilder()
        if (firstWord.endsWith('.')) {
            // End of sentence, make it a final state
            transitionsFromBuilder.mutableProbabilitiesByState.put(firstWord, 1.0)
        } else {
            // Compute the relative frequencies and create transitions accordingly
            val totalFollows = followCounts.values.sum()
            for ((secondWord, occurrences) in followCounts) {
                transitionsFromBuilder.mutableProbabilitiesByState.put(secondWord, occurrences / totalFollows.toDouble())
            }
        }

        // Insert transitions for word
        markovBuilder.mutableTransitions.put(firstWord, transitionsFromBuilder.build())

        // Update status vars
        val percentage = 100 * (runs + 1) / size.toDouble()
        if (percentage > lastPercentage + 1) {
            lastPercentage = percentage.toInt()
            println("Generated ${runs + 1}/$size states ($percentage %)")
        }
        runs++
    }
    return markovBuilder.build()
}

/**
 * Merge two results of [mergeWordFollowCountsByPrevious] by summing counts up.
 */
fun MutableMap<String, HashMap<String, Int>>.mergeWordFollowCountsByPrevious(other: Map<String, HashMap<String, Int>>) {
    this.keys.asSequence().filter { it in other }.forEach {
        this[it]!!.mergeWith(other[it]!!, Int::plus)
    }
    other.keys.asSequence().filter { it !in this }.forEach {
        this[it] = other[it]!!
    }
}

/**
 * Compute the markov chain resulting from the words in the [Config.generalTrainingCorpusPath].
 * This is a very expensive operation.
 */
fun computeMarkovChain(wordsToProcess: Int): MarkovChain {
    // Words mapped to their successors and counts of occurrences of the respective pairs
    var mergedWordFollowCountsByPrevious: HashMap<String, HashMap<String, Int>> = HashMap()

    // The number of words that have been processed
    val processedWords = AtomicLong(0)

    // The number of threads to use
    val threads = Runtime.getRuntime().availableProcessors()

    // The number of words a thread needs to process
    val wordsPerThread = wordsToProcess / threads

    // In order not to load the whole file into RAM, only load chunks of this
    // size in words
    val wordsChunkSize = 5000000
    val startTimeNs = System.nanoTime()

    println("Using $threads threads.")
    (0 until threads).map { threadNo ->
        thread {
            println("Thread $threadNo started")
            val words = readWordsFromSentencesInCorpus(maxWords = wordsPerThread.toLong(), skipWords = threadNo * wordsPerThread.toLong())
            val wordsChunks = words.chunkify(wordsChunkSize)
            for (wordsChunk in wordsChunks) {
                var chunkSize = 0 // Stores the actual chunk size
                val wordFollowCountsByPrevious = wordsChunk
                        .peek { chunkSize++ }
                        .toBigrams()
                        .filter { it.first.length < Config.wordMaxLength && it.second.length < Config.wordMaxLength }
                        .wordFollowCountsByPrevious()

                // Update the results with the result of this chunk
                synchronized(mergedWordFollowCountsByPrevious) {
                    mergedWordFollowCountsByPrevious.mergeWordFollowCountsByPrevious(wordFollowCountsByPrevious)
                }

                // Print status info
                val procWords = processedWords.addAndGet(chunkSize.toLong())
                val elapsedTimeNs = System.nanoTime() - startTimeNs
                val avgWordsPerSecond = procWords * 1e9 / elapsedTimeNs
                println("Thread $threadNo says: Thread-global AVG is $avgWordsPerSecond words/s.")
                println("Thread $threadNo says: Processed $procWords/$wordsToProcess (${100 * procWords / wordsToProcess} %) words.")
            }

            println("Thread $threadNo finished.")
        }
    }.forEach(Thread::join)

    println("Generating Markov Chain...")
    return mergedWordFollowCountsByPrevious.wordFollowCountsByPreviousToChain()
}