FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && useradd --create-home --uid 10001 varnuvi \
    && mkdir -p /home/varnuvi/.cache/huggingface \
    && chown -R varnuvi:varnuvi /home/varnuvi/.cache
COPY app.py .
USER varnuvi
ENV HF_HOME=/home/varnuvi/.cache/huggingface
EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1", "--limit-concurrency", "8", "--no-access-log"]
