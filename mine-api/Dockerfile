FROM openjdk:21-jdk-slim

LABEL maintainer="1102336460@qq.com"

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]