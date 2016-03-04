package de.cbruegg.mcl

import de.cbruegg.mcl.proto.MarkovChainOuterClass.MarkovChain
import java.util.*

/**
 * Extension property which holds states that don't have any reachable
 * other states.
 */
val MarkovChain.finalStates: Sequence<String>
    get() = wordsList.asSequence().filter { word ->
        (transitions[word]
                ?.probabilitiesByState
                ?.asSequence()
                ?.filter { secondWordWithProb -> secondWordWithProb.key != word }
                ?.map { it.value }
                ?.sumByDouble { it } ?: 0.0) == 0.0
    }

/**
 * Compute a random next state that can be reached using a transition.
 */
private fun MarkovChain.nextState(currentState: String, finalStates: HashSet<String>): String {
    if (currentState in finalStates || currentState !in transitions) {
        return currentState
    }

    val rand = Math.random()

    val transitions = transitions[currentState]!!.probabilitiesByState
    var intervalMarker = 0.0
    for (transition in transitions) {
        intervalMarker += transition.value
        if (intervalMarker - transition.value <= rand && rand < intervalMarker) {
            intervalMarker += transition.value
            return transition.key
        } else if (intervalMarker > 1) {
            throw IllegalStateException("Probabilities added up must not exceed 1")
        }
    }

    return currentState
}

/**
 * Simulate a sequence of state transitions in the chain.
 *
 * @param start Starting state
 * @param maxRuns Transitions after which should be aborted
 * @param finalStatesSet Set of final states. Ignore any transitions for states in this set.
 */
fun MarkovChain.simulateSequence(start: String = wordsList.getRandom(), maxRuns: Int? = null, finalStatesSet: HashSet<String> = finalStates.toHashSet()): Sequence<String> {
    var runs = 0
    var state = start

    return generateSequence(state) {
        if (maxRuns != null && runs++ < maxRuns || state !in finalStatesSet) {
            state = nextState(state, finalStatesSet)
            return@generateSequence state
        }

        return@generateSequence null
    }
}