FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY pom.xml .
RUN apk add --no-cache maven \
    && mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

EXPOSE 8000

ENTRYPOINT ["java","-jar","target/payment-gateway-0.0.1-SNAPSHOT.jar"]
