FROM eclipse-temurin:21
WORKDIR /accountService

COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src

ENTRYPOINT ["./mvnw", "spring-boot:run"]
