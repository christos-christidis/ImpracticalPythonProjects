package palingrams

import extensions.isPalindrome
import java.io.File

private const val DICTIONARY_FILE = "/usr/share/dict/american-english"

// TODO: for greek, doing it properly without accents will complicate the beautiful algorithm. Same if I try to ignore apostrophes
//private const val DICTIONARY_FILE = "/usr/share/dict/greek"

private const val NUM_PALINGRAMS = 10

fun main() {
    val words = File(DICTIONARY_FILE).readLines().filter { it.length > 1 }.toSet()

    val palingrams = mutableSetOf<String>()

    for (word in words) {
        if (word.isPalindrome()) {
            continue
        }

        val len = word.length

        // eg NURSES RUN (NUR = firstPart)
        for (end in 1 until len) {
            val firstPart = word.slice(0..end)
            if (firstPart.reversed() in words) {
                if (end == len - 1) {
                    palingrams.add(word + " " + word.reversed())
                } else {
                    val secondPart = word.slice((end + 1) until len)
                    if (secondPart.isPalindrome()) {
                        palingrams.add(word + " " + firstPart.reversed())
                    }
                }
            }
        }

        for (start in (len - 2) downTo 0) {
            val secondPart = word.slice(start until len)
            if (secondPart.reversed() in words) {
                if (start == 0) {
                    palingrams.add(word.reversed() + " " + word)
                } else {
                    val firstPart = word.slice(0 until start)
                    if (firstPart.isPalindrome()) {
                        palingrams.add(secondPart.reversed() + " " + word)
                    }
                }
            }
        }
    }

    println("\nFound ${palingrams.size} palingrams. The $NUM_PALINGRAMS longest are:\n")

    val longestPalingrams = palingrams.sortedByDescending { it.length }.slice(0 until NUM_PALINGRAMS)
    for (word in longestPalingrams) {
        println(word)
    }
}