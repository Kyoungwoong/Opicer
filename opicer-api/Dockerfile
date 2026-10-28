# syntax=docker/dockerfile:1
# API image. Tests run in CI, so the image build skips them. Choose the profile at run time:
#   docker run -e SPRING_PROFILES_ACTIVE=dev ... (local is the default)

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
	&& find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 opicer && mkdir -p /app/storage && chown opicer /app/storage
COPY --from=build /app/app.jar app.jar
USER opicer
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
