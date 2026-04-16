FROM maven:3.8.8-openjdk-17-slim AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -U

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=builder /app/target/syn-data-backend-1.0.0.jar .

EXPOSE 8085

CMD ["java", "-jar", "syn-data-backend-1.0.0.jar"]