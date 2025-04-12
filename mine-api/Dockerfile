FROM openjdk:21-jdk-slim

LABEL maintainer="1102336460@qq.com"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]