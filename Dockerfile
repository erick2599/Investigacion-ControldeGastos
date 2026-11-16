# Imagen mínima para validar el Gradle Wrapper.
# NO incluye el SDK de Android, así que no compila la aplicación por sí sola.
# Para compilar usa Android Studio o el SDK local (ver Docs/entorno.md).
FROM eclipse-temurin:17-jdk

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

RUN chmod +x gradlew

CMD ["./gradlew", "--version"]
