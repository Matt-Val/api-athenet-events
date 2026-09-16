# ---- Build ----
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---- Runtime ----
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN useradd --system --create-home --uid 1000 spring
USER spring

COPY --from=build /app/target/events-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
