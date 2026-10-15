import dev.voiceagent.app.insert.InsertionText

fun main() {
    val replace = InsertionText.merge("Hello world", 6, 11, "Varnuvi")
    check(replace.text == "Hello Varnuvi" && replace.cursor == 13)
    val reverse = InsertionText.merge("abcdef", 4, 2, "X")
    check(reverse.text == "abXef" && reverse.cursor == 3)
    val append = InsertionText.merge("Keep this", -1, -1, " too")
    check(append.text == "Keep this too")
    val invalid = InsertionText.merge("Keep this", 999, 999, "!")
    check(invalid.text == "Keep this!")
    val unicode = InsertionText.merge("A🙂B", 3, 3, "હાય")
    check(unicode.text == "A🙂હાયB" && unicode.cursor == 3 + "હાય".length)
    check(InsertionText.merge("", -1, -1, "Voice").text == "Voice")
    check(InsertionText.merge("Keep", 0, 4, "Replace").text == "Replace")
    check(InsertionText.merge("Unchanged", 4, 4, "").text == "Unchanged")
    println("PASS: cursor insertion, selected/reversed range replacement, unknown cursor, Unicode, content preservation")
}
