FROM openjdk:15-alpine
COPY target/dapla-dlp-pseudo-service-*.jar dapla-dlp-pseudo-service.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Dmicronaut.bootstrap.context=true", "-Xmx1g", "-jar", "dapla-dlp-pseudo-service.jar"]
