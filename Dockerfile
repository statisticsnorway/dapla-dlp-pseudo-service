FROM openjdk:17.0.1-slim-buster
RUN apt-get update && apt-get install -y curl
COPY target/dapla-dlp-pseudo-service-*.jar dapla-dlp-pseudo-service.jar
COPY target/classes/logback*.xml /conf/
EXPOSE 10210
CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-Dcom.sun.management.jmxremote", "-Dmicronaut.bootstrap.context=true", "-Xmx28g", "-jar", "dapla-dlp-pseudo-service.jar"]
