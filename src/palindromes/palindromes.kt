package palindromes

import extensions.isPalindrome
import java.io.File

//private const val DICTIONARY_FILE = "/usr/share/dict/american-english"
private const val DICTIONARY_FILE = "/usr/share/dict/greek"
private const val MAX_WORDS_PER_LINE = 10

fun main() {
    val words = File(DICTIONARY_FILE).readLines().filter { it.length > 1 }

    val palindromes = mutableListOf<String>()
    for (word in words) {
        if (word.toLowerCase().isPalindrome()) {
            palindromes.add(word)
        }
    }

    println("\nFound ${palindromes.size} palindromes:")
    printMaxWordsPerLine(palindromes)
    println()
}

private fun printMaxWordsPerLine(words: List<String>) {
    var count = 0
    for (word in words) {
        val sep = if (count < MAX_WORDS_PER_LINE - 1) " " else "\n"
        print("$word$sep")
        count++
        count %= MAX_WORDS_PER_LINE
    }
}
