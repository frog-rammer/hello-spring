# Dockerfile (repo root)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Maven 빌드 산출물(JAR)을 이미지에 포함
# Jenkins에서 mvn package 후 target/*.jar 가 생성됨
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
