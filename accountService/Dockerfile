FROM eclipse-temurin:21
WORKDIR /accountService

COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src

EXPOSE 8080

ENTRYPOINT ["./mvnw", "spring-boot:run"]
