FROM adoptopenjdk/openjdk15:alpine
RUN apk --no-cache add curl
COPY target/dapla-dlp-pseudo-service-*.jar dapla-dlp-pseudo-service.jar
COPY target/classes/logback*.xml /conf/
CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-Dcom.sun.management.jmxremote", "-Dmicronaut.bootstrap.context=true", "-Xmx4g", "-jar", "dapla-dlp-pseudo-service.jar"]
EXPOSE 10210
