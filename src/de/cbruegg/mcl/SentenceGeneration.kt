package de.cbruegg.mcl

import de.cbruegg.mcl.proto.MarkovChainOuterClass.MarkovChain

/**
 * Print random sentences given by the words in the chain.
 *
 * @param count Number of sentences to print
 * @param minSentenceWordLength The minimum number of words a sentence must containg
 * @param start A string with which the sentence has to begin
 */
fun MarkovChain.printRandomSentences(count: Int, minSentenceWordLength: Int = 3, start: String? = null) {
    println("Computing final states...")
    val finalStates = finalStates.toHashSet()
    println("Starting simulation.")
    repeat(count) {
        val seq = simulateSequence(start ?: wordsList.getRandom(), finalStatesSet = finalStates).toList()
        if (seq.count() > minSentenceWordLength) {
            seq.forEach { print(it); print(' ') }
            println()
        }
    }
}