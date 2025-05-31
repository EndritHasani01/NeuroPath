# ---------- Build stage ----------------------------------------------------
FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

# ---------- Slim runtime image --------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Pass JVM flags via JAVA_OPTS if needed
ENV JAVA_OPTS="-Xms512m -Xmx1024m" \
    PORT=8080
EXPOSE $PORT

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
