# Imagen del microservicio usuarios (Spring Boot / Java 25).
# El JAR se compila antes con ./mvnw package (ver k8s/desplegar.sh) y aqui
# solo se empaqueta sobre un runtime ligero.
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/usuarios-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
