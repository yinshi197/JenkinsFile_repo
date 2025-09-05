FROM python:3.12-slim

WORKDIR /app
COPY . /app

CMD ["python3", "src/hello.py"]