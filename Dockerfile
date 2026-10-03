FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests || (apk add --no-cache maven && mvn package -DskipTests)

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S isai && adduser -S isai -G isai
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown isai:isai /app/uploads
USER isai
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
