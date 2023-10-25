FROM amazoncorretto:17.0.8

RUN mkdir -p /app
WORKDIR /app

# Import Amazon RDS root certificates (see https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL.html)
ADD target/classes/rds-ca-2019-root.pem rds-ca-2019-root.pem
RUN keytool -import -alias aws2019 -file rds-ca-2019-root.pem -noprompt -keystore /usr/lib/jvm/java/lib/security/cacerts -storepass changeit

# Fetch datadog tracer
RUN curl --silent -Lq "https://dtdg.co/latest-java-tracer" -o dd-java-agent.jar

COPY target/healthtracker-rest-api-*.jar healthtracker-rest-api.jar

EXPOSE 8080

CMD ["java", "-javaagent:./dd-java-agent.jar", "-Djava.security.egd=file:/dev/./urandom", "-jar", "healthtracker-rest-api.jar"]