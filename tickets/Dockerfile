# Imagen del microservicio tickets (Spring Boot / Java 25).
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/tickets-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
