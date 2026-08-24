#!/bin/bash

# 에러 나면 즉시 멈춤. 안전장치
set -e

echo "👉 Git Pull (최신 코드 가져오기)..."
git pull origin main

echo "👉  Build JAR (빌드 시작)..."
./mvnw clean package -DskipTests

docker-compose rm -s -f backend

echo "👉  Docker Deploy (서버 재시작)..."
docker-compose up -d --build

echo "👉  Cleanup (청소)..."
docker image prune -f

echo "배포 완료"