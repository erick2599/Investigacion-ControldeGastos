# Entorno reproducible para compilar y probar el prototipo (Android / Gradle)
FROM eclipse-temurin:17-jdk

ARG GRADLE_VERSION=8.5
ARG CMDLINE_TOOLS_BUILD=11076708
ARG ANDROID_PLATFORM=android-34
ARG ANDROID_BUILD_TOOLS=34.0.0

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_HOME=/opt/gradle
ENV PATH="${PATH}:${GRADLE_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Gradle (versión fija)
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt \
 && mv "/opt/gradle-${GRADLE_VERSION}" "${GRADLE_HOME}" \
 && rm /tmp/gradle.zip

# Android command-line tools + plataforma y build-tools fijados
RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
 && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip" -o /tmp/cmdtools.zip \
 && unzip -q /tmp/cmdtools.zip -d "${ANDROID_HOME}/cmdline-tools" \
 && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
 && rm /tmp/cmdtools.zip \
 && yes | sdkmanager --licenses > /dev/null \
 && sdkmanager "platform-tools" "platforms;${ANDROID_PLATFORM}" "build-tools;${ANDROID_BUILD_TOOLS}"

WORKDIR /workspace
COPY . .

# Compila el APK de depuración y ejecuta las pruebas unitarias
CMD ["gradle", "--no-daemon", "assembleDebug", "testDebugUnitTest"]
