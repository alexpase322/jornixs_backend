FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
FROM tomcat:10.1-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080

CMD ["catalina.sh", "run"]

ENTRYPOINT ["java", "-jar", "app.jar"]