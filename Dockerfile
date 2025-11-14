# // TODO: Implementar el Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /data
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]