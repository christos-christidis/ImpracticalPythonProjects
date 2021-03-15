package extensions

import java.text.Normalizer

fun String.isPalindrome(): Boolean {
    val unaccented = this.removeAccents()
    return unaccented == unaccented.reversed()
}

fun String.removeAccents(): String {
    val unaccentRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    val normalizedForm = Normalizer.normalize(this, Normalizer.Form.NFD)
    return unaccentRegex.replace(normalizedForm, "")
}

