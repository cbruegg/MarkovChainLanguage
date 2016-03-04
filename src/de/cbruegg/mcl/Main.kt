package de.cbruegg.mcl

import com.google.protobuf.CodedInputStream
import de.cbruegg.mcl.proto.MarkovChainOuterClass.MarkovChain

fun computeAndSaveChain() {
    val chain = computeMarkovChain(175000000)

    val chainFileOutput = Config.chainPath.toFile().outputStream().buffered()
    chainFileOutput.use {
        println("Writing to file...")
        chain.writeTo(chainFileOutput)
        println("Finished!")
    }
}

fun printRandomText() {
    val chainInput = Config.chainPath.toFile().inputStream()
    val codedInputStream = CodedInputStream.newInstance(chainInput)
    codedInputStream.setSizeLimit(Int.MAX_VALUE)
    println("Loading Markov chain...")
    val chainProto = chainInput.use {
        MarkovChain.parseFrom(codedInputStream)
    }
    chainProto.printRandomSentences(40, start = "a")
}

fun main(args: Array<String>) {
    // preprocessRedditComments()
    // computeAndSaveChain()
    printRandomText()
}