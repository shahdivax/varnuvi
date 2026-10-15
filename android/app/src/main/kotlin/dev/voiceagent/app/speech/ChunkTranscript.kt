package dev.voiceagent.app.speech

/** Main-thread ledger: each captured chunk has one slot, independent of response order. */
class ChunkTranscript {
    enum class State { PENDING, COMPLETE, FAILED }
    private data class Part(var state: State = State.PENDING, var text: String = "")
    private val parts = mutableListOf<Part>()
    val size: Int get() = parts.size
    val pending: Int get() = parts.count { it.state == State.PENDING }
    val completed: Int get() = parts.count { it.state == State.COMPLETE }
    val failed: List<Int> get() = parts.indices.filter { parts[it].state == State.FAILED }
    fun add(): Int { parts.add(Part()); return parts.lastIndex }
    fun complete(index: Int, text: String) {
        check(parts[index].state == State.PENDING)
        require(text.isNotBlank())
        parts[index].text = text.trim()
        parts[index].state = State.COMPLETE
    }
    fun fail(index: Int) { check(parts[index].state == State.PENDING); parts[index].state = State.FAILED }
    fun retry(index: Int): Boolean {
        if (parts[index].state != State.FAILED) return false
        parts[index].state = State.PENDING
        return true
    }
    fun text(): String = parts.filter { it.state == State.COMPLETE }.joinToString(" ") { it.text }
}
