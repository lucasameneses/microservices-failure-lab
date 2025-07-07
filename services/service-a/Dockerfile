FROM eclipse-temurin:17-jdk-alpine
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src ./src
CMD ["./mvnw", "spring-boot:run"]