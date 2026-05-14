FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY presentation_analyzer/requirements.txt ./requirements.txt

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ffmpeg \
        wget \
        libgl1 \
        libgles2 \
        libegl1 \
        libglib2.0-0t64 \
    && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["python", "run_api.py", "--host", "0.0.0.0", "--port", "8000"]
