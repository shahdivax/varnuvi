import dev.voiceagent.app.writing.WritingPolicy
fun main() {
    val defaults = WritingPolicy.emptyDefaults()
    check(defaults.size == 5)
    for (enabled in listOf(false, true)) for (tested in listOf(false, true)) for (configured in listOf(false, true)) {
        check(WritingPolicy.shouldProcess(enabled, tested, configured) == (enabled && tested && configured))
    }
    check(!WritingPolicy.shouldProcess(false, true, true)) { "Off must bypass AI even with saved preferences" }
    check(WritingPolicy.shouldProcess(true, true, true)) { "On must process spoken instructions even with all defaults None" }
    check(defaults.all { (field, value) -> value in WritingPolicy.options.getValue(field) })
    println("PASS: AI runs only when enabled, tested, and configured; defaults do not override the switch")
}
