FROM eclipse-temurin:21-jre-jammy
WORKDIR /tmp
COPY target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]