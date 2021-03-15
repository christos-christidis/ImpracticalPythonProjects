package poor_mans_bar_chart

import extensions.removeAccents
import org.jsoup.Jsoup

private const val URL = "https://www.gutenberg.org/files/28658/28658-h/28658-h.htm"
private const val LANG = "gr"

@Suppress("ConstantConditionIf")
private fun getFreqDictionary(text: String): Map<Char, Int> {
    val freqDictionary = mutableMapOf<Char, Int>()
    val letterRange = if (LANG == "gr") 'α'..'ω' else 'a'..'z'

    for (c in text) {
        if (c in letterRange) {
            freqDictionary[c] = freqDictionary.getOrDefault(c, 0) + 1
        }
    }

    return freqDictionary
}

@Suppress("ConstantConditionIf")
private fun printBarChart(freqDictionary: Map<Char, Int>) {
    println()

    val maxCount = freqDictionary.values.max()!!

    val letterRange = if (LANG == "gr") 'α'..'ω' else 'a'..'z'

    for (c in letterRange) {
        val count = freqDictionary.getOrDefault(c, 0)
        val barLength = (count / maxCount.toDouble() * 80).toInt()
        println("'$c' : ${"#".repeat(barLength)} ($count)")
    }
}

@Suppress("ConstantConditionIf")
fun main() {
    val doc = Jsoup.connect(URL).get()
    var text = doc.body().text().toLowerCase()

    if (LANG == "gr") {
        text = text.removeAccents()
    }

    val freqDictionary = getFreqDictionary(text)
    printBarChart(freqDictionary)
}
