package anagrams

import java.io.File

private const val DICTIONARY_FILE = "/usr/share/dict/american-english"

// We don't want to leave a single-letter alone, since we've excluded single-letter words from our list
private fun String.canBeDerivedFrom(availableChars: String): Boolean {
    val charList = availableChars.toMutableList()
    val word = this.replace("'", "")
    for (c in word) {
        if (c !in charList) {
            return false
        } else {
            charList.remove(c)
        }
    }

    return true
}

private fun String.removeChars(word: String): String {
    val charList = this.toMutableList()
    for (c in word) {
        charList.remove(c)
    }

    return charList.fold("", { acc, c -> acc + c })
}

// NOTE: results will include 'foobar wow', but not 'wow foobar'. To "fix" that, I'd have to pass all words in the
// recursive call , not a slice. But then I get no new info and the running time becomes too high
private fun findAnagrams(
    remainingChars: String, words: List<String>, partialAnagram: MutableList<String>, anagramWords: MutableList<String>
) {
    if (remainingChars.isEmpty()) {
        anagramWords.add(partialAnagram.joinToString(" "))
        return
    }

    for ((idx, word) in words.withIndex()) {
        if (word.canBeDerivedFrom(remainingChars)) {
            partialAnagram.add(word)
            findAnagrams(
                remainingChars.removeChars(word), words.slice(idx + 1 until words.size),
                partialAnagram, anagramWords
            )
            partialAnagram.removeAt(partialAnagram.size - 1)
        }
    }
}

fun main() {
    val words = File(DICTIONARY_FILE).readLines().filter { it.length > 1 }.sortedByDescending { it.length }

    print("\nPlease enter a word: ")
    val userWord = readLine()!!.replace(" ", "").toLowerCase()

    val anagramWords = mutableListOf<String>()

    findAnagrams(userWord, words, mutableListOf(), anagramWords)

    if (anagramWords.isNotEmpty()) {
        println("${anagramWords.size} anagrams: " + anagramWords.joinToString(", "))
    } else {
        println("\nFound no anagrams!")
    }
}
