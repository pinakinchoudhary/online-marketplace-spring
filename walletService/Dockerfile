FROM eclipse-temurin:21
WORKDIR /walletService

COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src

ENTRYPOINT ["./mvnw", "spring-boot:run"]
