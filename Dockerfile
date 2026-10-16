FROM eclipse-temurin:17-jdk

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

RUN chmod +x gradlew

COPY . .

CMD ["./gradlew", "--version"]
