package de.cbruegg.mcl

import java.io.File

object Config {

    /**
     * Path to the Reddit comment dump
     */
    val redditCorpusPath = File("""C:\Users\Christian\Documents\reddit_comments\RC_2015-01""").toPath()

    /**
     * Path to a general corpus path for training. It has to consist of newline-separated words, of which
     * some must end with a dot to indicate a sentence end.
     */
    val generalTrainingCorpusPath = File("""C:\Users\Christian\Documents\reddit_comments\RC_2015-01.words""").toPath()

    /**
     * Path to a chain (to be) generated with this program.
     */
    val chainPath = File("""C:\Users\Christian\Documents\reddit_comments\RC_2015-01.chain""").toPath()

    /**
     * Characters marking a word end. Do not include a dot here as it's used for specifying final states
     * in the markov chain.
     */
    val wordDelimiters = charArrayOf('\n', ' ', ',')

    /**
     * The maximum number of characters a word in the chain may have.
     */
    val wordMaxLength = 25
}