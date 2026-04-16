FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/auth-server-1.0.0.jar /app/auth-server.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "auth-server.jar"]
