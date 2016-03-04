package de.cbruegg.mcl

import org.codehaus.jackson.map.ObjectMapper
import java.io.File
import java.nio.file.Files

private val jsonMapper = ObjectMapper()

/**
 * Return a sequence of comments from Reddit. Comments are separated by newlines in the file.
 *
 * @param skipComments The number of comments to skip reading
 * @param maxComments The maximum number of comments to return
 */
fun readRedditComments(skipComments: Long = 0, maxComments: Long = Long.MAX_VALUE): Sequence<String> =
        Files.lines(Config.redditCorpusPath)
                .skip(skipComments)
                .limit(maxComments)
                .asSequence()
                .map { jsonMapper.readValue(it, Map::class.java)["body"] as String }

/**
 * Read words as they appear in the sentences in the general corpus. ([Config.generalTrainingCorpusPath])
 *
 * @param The number of words to skip
 * @param maxWords The maximum number of words to return
 */
fun readWordsFromSentencesInCorpus(skipWords: Long = 0, maxWords: Long = Long.MAX_VALUE): Sequence<String> =
        Files.lines(Config.generalTrainingCorpusPath)
                .skip(skipWords)
                .limit(maxWords)
                .asSequence()
                .map(String::intern) // Avoid bloating the memory

/**
 * Convert the reddit comments file to a file processable [readWordsFromSentencesInCorpus].
 */
fun preprocessRedditComments(outFile: File = Config.generalTrainingCorpusPath.toFile()) {
    val out = outFile.outputStream().writer().buffered(16784)
    out.use {
        val words = readRedditComments().extractWordsFromCommentTexts()
        out.write("")
        words.forEach {
            if (it.isNotEmpty()) {
                out.appendln(it)
            }
        }
    }
}

/**
 * Transform a list of comments into a flattened list of words
 * as they appear in the comments. Additionally, words are made lowercase
 * and trimmed.
 */
private fun Sequence<String>.extractWordsFromCommentTexts(): Sequence<String>
        = flatMap { it.splitToSequence(*Config.wordDelimiters) }.map { it.toLowerCase().trim() }