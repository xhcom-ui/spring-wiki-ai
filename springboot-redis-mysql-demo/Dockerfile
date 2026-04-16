FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/springboot-redis-mysql-demo-1.0.0.jar /app/springboot-redis-mysql-demo.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "springboot-redis-mysql-demo.jar"]
