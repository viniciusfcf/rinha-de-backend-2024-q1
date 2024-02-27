./mvnw clean package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t viniciusfcf/rinha-backend-2024q1-jvm:latest .