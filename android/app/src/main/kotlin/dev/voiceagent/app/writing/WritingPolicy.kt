package dev.voiceagent.app.writing

/** None means no preset instruction, not disabled AI. */
object WritingPolicy {
    val options = mapOf(
        "action" to setOf("none", "translate", "summary", "paraphrase"),
        "inputLanguage" to setOf("auto", "en", "gu", "hi", "mr", "ta", "te", "bn", "es", "fr", "de", "ja"),
        "outputLanguage" to setOf("auto", "English", "Gujarati", "Hindi", "Marathi", "Tamil", "Telugu", "Bengali", "Spanish", "French", "German", "Japanese"),
        "tone" to setOf("none", "professional", "friendly", "formal", "casual", "direct"),
        "style" to setOf("none", "paragraphs", "bullet points", "email", "short message"),
    )
    fun emptyDefaults() = options.keys.associateWith { if (it.endsWith("Language")) "auto" else "none" }
    fun shouldProcess(enabled: Boolean, tested: Boolean, configured: Boolean) = enabled && tested && configured
}
