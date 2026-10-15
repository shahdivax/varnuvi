import dev.voiceagent.app.accessibility.BubbleRefreshPolicy

fun main() {
    for (visible in listOf(false, true)) {
        check(BubbleRefreshPolicy.delay(false, true, false, visible) == null) { "Screen off must not keep a timer" }
        check(BubbleRefreshPolicy.delay(true, false, false, visible) == null) { "No enabled apps must not keep a timer" }
        check(BubbleRefreshPolicy.delay(true, true, true, visible) == null) { "Recording must not scan other windows" }
    }
    check(BubbleRefreshPolicy.delay(true, true, false, true) == 500L)
    check(BubbleRefreshPolicy.delay(true, true, false, false) == 5000L)
    println("PASS: bubble recovery sleeps while screen off, disabled, or dictating; dormant checks back off")
}
