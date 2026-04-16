FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/sharing-sphere-demo-1.0.0.jar /app/sharing-sphere-demo.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "sharing-sphere-demo.jar"]
