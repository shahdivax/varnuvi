import dev.voiceagent.app.speech.TranscriptionEndpoint

fun main() {
    for (url in listOf("https://speech.example.org/api/transcribe", "http://localhost:8000/api/transcribe", "http://127.0.0.1:8000/api/transcribe", "http://10.0.2.2:8000/api/transcribe")) {
        check(TranscriptionEndpoint.validate(url) == url)
    }
    for (url in listOf("", "https://x.xx.xxx/api/transcribe", "http://speech.example.org/api/transcribe", "https://user:pass@speech.example.org/api/transcribe", "https://speech.example.org/api/transcribe?token=secret", "https://speech.example.org/api/transcribe#fragment", "https://speech.example.org/api/transcribe/", "file:///api/transcribe", "https://speech.example.org:99999/api/transcribe")) {
        check(runCatching { TranscriptionEndpoint.validate(url) }.isFailure) { "Unsafe endpoint accepted: $url" }
    }
    println("PASS: HTTPS endpoints, local-only HTTP exceptions, placeholders, credentials/query/fragment and port validation")
}
