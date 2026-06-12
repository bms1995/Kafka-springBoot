FROM eclipse-temurin:21-jre
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY target/inventory-service-1.0.0.jar app.jar
USER spring:spring
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
