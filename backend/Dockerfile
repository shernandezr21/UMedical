# ---- Etapa 1: compilar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# maven.test.skip: ni compila ni ejecuta las pruebas (la imagen no las necesita)
RUN mvn -q -B -Dmaven.test.skip=true package

# ---- Etapa 2: ejecutar (usuario sin privilegios) ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --home /app app \
 && mkdir -p /app /data/uploads && chown -R app:app /app /data
WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
