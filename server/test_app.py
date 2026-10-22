"""API contract/security checks using synthetic audio; no model downloads or inference."""
import io
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
import wave

os.environ['TRANSCRIPTION_TOKEN'] = 'synthetic-test-token-' + '0' * 32
sys.path.insert(0, str(Path(__file__).parent))
import app
from fastapi.testclient import TestClient


def wav(seconds=1):
    output = io.BytesIO()
    with wave.open(output, 'wb') as audio:
        audio.setnchannels(1); audio.setsampwidth(2); audio.setframerate(16000)
        audio.writeframes(b'\0\0' * (seconds * 16000))
    return output.getvalue()


class ApiChecks(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app.app)
        self.headers = {'Authorization': 'Bearer ' + os.environ['TRANSCRIPTION_TOKEN']}

    def send(self, content=None, language='auto'):
        return self.client.post('/api/transcribe', headers=self.headers,
            files={'audio': ('chunk.wav', wav() if content is None else content, 'audio/wav')}, data={'language': language})

    def test_authentication_and_health(self):
        self.assertEqual(self.client.get('/health').status_code, 401)
        self.assertEqual(self.client.get('/health', headers=self.headers).json()['status'], 'ok')
        with patch.object(app, 'transcribe') as infer:
            self.assertEqual(self.client.post('/api/transcribe', files={'audio': ('audio', wav())}).status_code, 401)
            infer.assert_not_called()

    def test_contract_and_cleanup(self):
        paths = []
        def infer(path, language):
            self.assertTrue(path.exists()); self.assertEqual(language, 'gu'); paths.append(path)
            return {'text': 'ગુજરાતી words', 'language': 'gu'}
        with patch.object(app, 'transcribe', side_effect=infer):
            response = self.send(language='gu')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['text'], 'ગુજરાતી words')
        self.assertFalse(paths[0].exists())

    def test_invalid_audio_and_bounds(self):
        with patch.object(app, 'transcribe') as infer:
            self.assertEqual(self.send(b'not audio').status_code, 422)
            self.assertEqual(self.send(wav(32)).status_code, 422)
            self.assertEqual(self.send(language='not-a-language').status_code, 422)
            self.assertEqual(self.send(b'x' * (app.MAX_BODY + 1)).status_code, 413)
            infer.assert_not_called()

    def test_failure_cleanup_and_busy(self):
        paths = []
        def fail(path, language):
            paths.append(path); raise RuntimeError('synthetic internal failure')
        with patch.object(app, 'transcribe', side_effect=fail):
            response = self.send()
        self.assertEqual(response.status_code, 503)
        self.assertNotIn('synthetic internal failure', response.text)
        self.assertFalse(paths[0].exists())
        with patch.object(app._slots, 'acquire', return_value=False):
            self.assertEqual(self.send().status_code, 429)

if __name__ == '__main__':
    unittest.main()
