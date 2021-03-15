package haiku

import java.io.File

fun main() {
    val syllableDict = mutableMapOf<String, Int>()
    loadCmuWords(syllableDict)
    loadMissingWords(syllableDict)

    val haikuTrainingWords = File("src/haiku/haiku_training_data.txt").readText()
        .replace('-', ' ').replace("  ", " ").split(" ")
        .map { it.replace("^\\W+|\\W+$".toRegex(), "") }

    val markovModelFirstOrder = createMarkovModelFirstOrder(haikuTrainingWords)
    val markovModelSecondOrder = createMarkovModelSecondOrder(haikuTrainingWords)

    println("\nReady for haikus? (y/n)")
    if (readLine() == "n") {
        return
    }

    while (true) {
        val firstLine = generateFirstLine(markovModelFirstOrder, markovModelSecondOrder, syllableDict)
        val secondLine = generateSecondOrThirdLine(firstLine, markovModelSecondOrder, syllableDict, 7)
        val thirdLine = generateSecondOrThirdLine(secondLine, markovModelSecondOrder, syllableDict, 5)
        println(firstLine)
        println(secondLine)
        println(thirdLine)

        println("\nYou want more? (y/n)")
        if (readLine() == "n") {
            break
        }
    }
}

private fun loadCmuWords(syllableDict: MutableMap<String, Int>) {
    val dictLines = File("src/haiku/cmudict-0.7b.txt").readLines()
        .map { it.replace("  ", " ") }

    for (line in dictLines) {
        val splitLine = line.split(" ")
        var numSyllables = 0
        for (phoneme in splitLine.slice(1 until splitLine.size)) {
            if (phoneme.last().isDigit()) {
                numSyllables += 1
            }
        }
        syllableDict[splitLine[0]] = numSyllables
    }
}

private fun loadMissingWords(syllableDict: MutableMap<String, Int>) {
    val missingWordsLines = File("src/haiku/missing_words.txt").readLines()
    for (line in missingWordsLines) {
        syllableDict[line.split(" ")[0]] = line.split(" ")[1].toInt()
    }
}

private fun createMarkovModelFirstOrder(trainingWords: List<String>): Map<String, List<String>> {
    val model = mutableMapOf<String, MutableList<String>>()
    for (i in 0 until trainingWords.size - 1) {
        val first = trainingWords[i]
        val second = trainingWords[i + 1]
        if (model[first] == null) {
            model[first] = mutableListOf()
        }
        model[first]?.add(second)
    }

    return model
}

private fun createMarkovModelSecondOrder(trainingWords: List<String>): Map<String, List<String>> {
    val model = mutableMapOf<String, MutableList<String>>()
    for (i in 0 until trainingWords.size - 2) {
        val first = trainingWords[i] + " " + trainingWords[i + 1]
        val second = trainingWords[i + 2]
        if (model[first] == null) {
            model[first] = mutableListOf()
        }
        model[first]?.add(second)
    }

    return model
}

private fun Map<String, Int>.numSyllables(phrase: String): Int {
    var numSyllables = 0

    val words = phrase.replace('-', ' ').toUpperCase().split(" ")
    for (word in words) {
        var preparedWord = word
        if (word.endsWith("'S")) {
            preparedWord = word.slice(0 until word.length - 2)
        }
        numSyllables += this[preparedWord]!!
    }

    return numSyllables
}

private fun generateFirstLine(
    markovFirstOrder: Map<String, List<String>>, markovSecondOrder: Map<String, List<String>>,
    syllableDict: Map<String, Int>
): String {
    var remainingSyllables = 5
    // We want to avoid 1-word lines so...
    val (firstWord, numSyllables) = getRandomSeed(markovFirstOrder, syllableDict, remainingSyllables - 1)
    val firstLine = mutableListOf(firstWord)
    remainingSyllables -= numSyllables

    val (secondWord, numSyllables2) = getWordFromMarkovModel(
        firstWord, markovFirstOrder, syllableDict, remainingSyllables
    )
    firstLine.add(secondWord)
    remainingSyllables -= numSyllables2

    while (remainingSyllables > 0) {
        val previousTwoWords = firstLine.joinToString(" ").lastTwoWords()
        val (nextWord, numSyllables3) = getWordFromMarkovModel(
            previousTwoWords, markovSecondOrder, syllableDict, remainingSyllables
        )
        firstLine.add(nextWord)
        remainingSyllables -= numSyllables3
    }

    return firstLine.joinToString(" ")
}

// returns a word if markovFirst, else a phrase of two words
private fun getRandomSeed(
    markovModel: Map<String, List<String>>, syllableDict: Map<String, Int>, maxSyllables: Int
): Pair<String, Int> {
    var phrase = "a phrase which contains too many syllables"
    while (syllableDict.numSyllables(phrase) > maxSyllables) {
        phrase = markovModel.keys.random()
    }
    return Pair(phrase, syllableDict.numSyllables(phrase))
}

private fun getWordFromMarkovModel(
    phrase: String, markovModel: Map<String, List<String>>, syllableDict: Map<String, Int>, maxSyllables: Int
): Pair<String, Int> {
    var listOfNextWords = markovModel[phrase] ?: emptyList()
    if (listOfNextWords.isEmpty()) {
        val (randomPhrase, _) = getRandomSeed(markovModel, syllableDict, 8)
        listOfNextWords = markovModel[randomPhrase] ?: error("$randomPhrase not found in markov model!")
    }

    var suitableWords = listOfNextWords.filter { syllableDict.numSyllables(it) <= maxSyllables }

    while (suitableWords.isEmpty()) {
        // we don't care about the syllables of the seed word/words
        val (randomPhrase, _) = getRandomSeed(markovModel, syllableDict, 8)
        listOfNextWords = markovModel[randomPhrase] ?: error("'$randomPhrase' not found in markov model!")
        suitableWords = listOfNextWords.filter { syllableDict.numSyllables(it) <= maxSyllables }
    }

    val pickedWord = suitableWords.random()
    return Pair(pickedWord, syllableDict.numSyllables(pickedWord) ?: error("'$pickedWord' not found in syllableDict!"))
}

private fun generateSecondOrThirdLine(
    previousLine: String, markovSecondOrder: Map<String, List<String>>, syllableDict: Map<String, Int>,
    maxSyllables: Int
): String {
    var remainingSyllables = maxSyllables
    // We want to avoid 1-word lines
    val (firstWord, numSyllables) = getWordFromMarkovModel(
        previousLine.lastTwoWords(), markovSecondOrder, syllableDict, remainingSyllables - 1
    )
    val line = mutableListOf(firstWord)
    remainingSyllables -= numSyllables

    val nextSeed = previousLine.split(" ").last() + " " + firstWord

    val (secondWord, numSyllables2) = getWordFromMarkovModel(
        nextSeed, markovSecondOrder, syllableDict, remainingSyllables
    )
    line.add(secondWord)
    remainingSyllables -= numSyllables2

    while (remainingSyllables > 0) {
        val previousTwoWords = line.joinToString(" ").lastTwoWords()
        val (nextWord, numSyllables3) = getWordFromMarkovModel(
            previousTwoWords, markovSecondOrder, syllableDict, remainingSyllables
        )
        line.add(nextWord)
        remainingSyllables -= numSyllables3
    }

    return line.joinToString(" ")
}

private fun String.lastTwoWords(): String {
    val words = this.split(" ")
    if (words.size < 2) {
        throw IllegalArgumentException("lastTwoWords() called on string with < 2 words: $this")
    }
    return words.slice(words.size - 2 until words.size).joinToString(" ")
}

