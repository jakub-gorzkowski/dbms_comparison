FROM gradle:8.13-jdk AS build

WORKDIR /app

COPY settings.gradle.kts gradlew gradlew.bat /app/
COPY gradle /app/gradle

COPY src /app/src
COPY build.gradle.kts /app/

RUN gradle shadowJar --no-daemon

FROM openjdk:24

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

CMD ["sh", "-c", "java -jar app.jar && tail -f /dev/null"]