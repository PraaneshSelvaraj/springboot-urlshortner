FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Build grpc-common
COPY grpc-common/pom.xml grpc-common/pom.xml
RUN cd grpc-common && mvn dependency:go-offline
COPY grpc-common/src/ grpc-common/src/
RUN cd grpc-common && mvn clean install -DskipTests

# Build rest-service
COPY rest-service/pom.xml .
RUN mvn dependency:go-offline
COPY rest-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
