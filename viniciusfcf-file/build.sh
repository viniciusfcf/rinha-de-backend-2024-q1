./mvnw clean package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t viniciusfcf/rinha-backend-file-2024q1-jvm:v1 .