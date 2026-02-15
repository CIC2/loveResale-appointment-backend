# Build stage
FROM eclipse-temurin:17 as build

RUN apt-get update && apt-get install -y maven

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .

RUN mvn package -Dmaven.test.skip=true

# Debug: list target files to verify jar name
RUN ls -l /build/target

# Runnable stage
FROM eclipse-temurin:17

RUN apt-get update && apt-get install -y fontconfig libfreetype6 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Use exact path here:
COPY --from=build /build/target/homefly-appointment-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8030

ENTRYPOINT ["java", "-jar", "app.jar"]
