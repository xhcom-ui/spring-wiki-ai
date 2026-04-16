FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/resource-server-1.0.0.jar /app/resource-server.jar

EXPOSE 9001

ENTRYPOINT ["java", "-jar", "resource-server.jar"]
