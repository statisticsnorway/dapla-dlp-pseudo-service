FROM azul/zulu-openjdk:17
RUN apt-get -qq update && apt-get -y --no-install-recommends install curl
COPY target/dapla-dlp-pseudo-service-*.jar dapla-dlp-pseudo-service.jar
COPY target/classes/logback*.xml /conf/
EXPOSE 10210
CMD ["java", "-Dcom.sun.management.jmxremote", "-Dmicronaut.bootstrap.context=true", "-jar", "dapla-dlp-pseudo-service.jar"]
