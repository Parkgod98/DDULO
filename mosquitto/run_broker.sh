#!/bin/bash
# Ticket-1: MQTT Broker 실행 스크립트 (SSL 적용)

echo "🛑 기존 컨테이너 정리 중..."
docker stop mqtt-broker 2>/dev/null
docker rm mqtt-broker 2>/dev/null

echo "🚀 MQTT Broker 실행 (Port 8000 -> 8883 SSL)..."
docker run -d \
  --name mqtt-broker \
  -p 8000:8883 \
  -v $(pwd)/config:/mosquitto/config \
  -v $(pwd)/data:/mosquitto/data \
  -v $(pwd)/log:/mosquitto/log \
  eclipse-mosquitto

echo "✅ 실행 완료! 로그 확인: docker logs -f mqtt-broker"
