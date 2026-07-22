# Imagen del microservicio asignaciones (Spring Boot / Java 25).
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/asignaciones-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
