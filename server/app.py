"""One authenticated WAV transcription endpoint for Varnuvi. No remote AI APIs."""
import hmac
import os
from pathlib import Path
import re
import tempfile
import threading
import wave

from fastapi import FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.responses import JSONResponse

MAX_BODY = 2 * 1024 * 1024
MODEL = os.environ.get('WHISPER_MODEL', 'small')
DEVICE = os.environ.get('WHISPER_DEVICE', 'cpu')
COMPUTE_TYPE = os.environ.get('WHISPER_COMPUTE_TYPE', 'int8')
_token = os.environ.get('TRANSCRIPTION_TOKEN', '')
if not 32 <= len(_token) <= 512 or _token.startswith('replace-') or not _token.isascii() or any(c.isspace() for c in _token):
    raise RuntimeError('Set TRANSCRIPTION_TOKEN to a random token of at least 32 characters before starting the server.')
_model = None
_inference = threading.Lock()
# One request can infer while a second waits (the app uploads two chunks at once).
_slots = threading.BoundedSemaphore(2)
app = FastAPI(title='Varnuvi speech server', docs_url=None, redoc_url=None, openapi_url=None)


@app.middleware('http')
async def protect(request: Request, call_next):
    if not hmac.compare_digest(request.headers.get('authorization', '').encode(), ('Bearer ' + _token).encode()):
        return JSONResponse({'detail': 'Unauthorized'}, status_code=401)
    if request.method == 'POST':
        try:
            length = int(request.headers.get('content-length', '0'))
        except ValueError:
            length = 0
        if not 0 < length <= MAX_BODY:
            return JSONResponse({'detail': 'Supply Content-Length; maximum request size is 2 MiB.'}, status_code=413)
    return await call_next(request)


@app.get('/health')
def health():
    return {'status': 'ok', 'model_loaded': _model is not None}


def validate_audio(path: Path):
    try:
        with wave.open(str(path), 'rb') as audio:
            if (audio.getnchannels(), audio.getsampwidth(), audio.getframerate(), audio.getcomptype()) != (1, 2, 16000, 'NONE'):
                raise ValueError('Expected mono 16 kHz PCM16 WAV.')
            frames = audio.getnframes()
            if not 0 < frames <= 31 * 16000 or len(audio.readframes(frames)) != frames * 2:
                raise ValueError('Expected a complete WAV chunk of at most 31 seconds.')
    except (wave.Error, EOFError) as error:
        raise ValueError('Invalid WAV audio.') from error


def transcribe(path: Path, language: str):
    global _model
    with _inference:
        if _model is None:
            from faster_whisper import WhisperModel
            _model = WhisperModel(MODEL, device=DEVICE, compute_type=COMPUTE_TYPE)
        segments, info = _model.transcribe(str(path), language=None if language == 'auto' else language,
            beam_size=5, vad_filter=True)
        text = ' '.join(segment.text.strip() for segment in segments if segment.text.strip()).strip()
        return {'text': text, 'language': info.language}


@app.post('/api/transcribe')
async def transcribe_upload(audio: UploadFile = File(...), language: str = Form('auto')):
    if language != 'auto' and not re.fullmatch(r'[a-z]{2,3}', language):
        raise HTTPException(422, 'Use auto or a Whisper language code.')
    if not _slots.acquire(blocking=False):
        raise HTTPException(429, 'Server is busy. Retry this chunk shortly.', headers={'Retry-After': '5'})
    try:
        with tempfile.TemporaryDirectory(prefix='varnuvi-audio-') as directory:
            path = Path(directory) / 'audio.wav'
            data = await audio.read(MAX_BODY + 1)
            if len(data) > MAX_BODY:
                raise HTTPException(413, 'Audio exceeds the upload limit.')
            path.write_bytes(data)
            try:
                validate_audio(path)
            except ValueError as error:
                raise HTTPException(422, str(error)) from error
            # Do not abandon inference on request cancellation: temp files remain
            # until the worker finishes, then the context removes them.
            from starlette.concurrency import run_in_threadpool
            try:
                return await run_in_threadpool(transcribe, path, language)
            except ValueError as error:
                raise HTTPException(422, 'The audio or language could not be transcribed.') from error
            except Exception as error:
                raise HTTPException(503, 'Speech model unavailable. Check the server configuration and model cache.') from error
    finally:
        try:
            await audio.close()
        finally:
            _slots.release()
