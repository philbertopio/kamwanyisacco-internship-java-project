

# Stage 1: Maven build
FROM docker.io/library/maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy POM first so Maven dependency layer is cached separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and produce the WAR
COPY src/ ./src/
RUN mvn clean package -DskipTests -B


#Stage 2: Tomcat 9 runtime
FROM docker.io/library/tomcat:9.0-jdk17-temurin

LABEL maintainer="Kimwanyi SACCO DevOps"
LABEL org.opencontainers.image.title="KimwanyiSacco"
LABEL org.opencontainers.image.description="Kimwanyi SACCO management application"

#  MySQL client tools (used by the readiness wait script)
RUN apt-get update -qq \
 && apt-get install -y --no-install-recommends default-mysql-client curl \
 && rm -rf /var/lib/apt/lists/*

# Remove default Tomcat apps to keep the image clean
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy built WAR as ROOT so the app is served at context path /KimwanyiSacco ─
COPY --from=builder /app/target/KimwanyiSacco.war \
     /usr/local/tomcat/webapps/KimwanyiSacco.war

# Entrypoint script: waits for MySQL then starts Tomcat
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Tomcat port
EXPOSE 8080
ENV DB_HOST=sacco-mysql \
    DB_PORT=3306 \
    DB_NAME=kimwanyisacco_db \
    DB_USERNAME=sacco_user \
    DB_PASSWORD=changeme \
    PAYMENT_CURRENCY=KES \
    BUSINESS_NAME="Kimwanyi SACCO"

ENTRYPOINT ["/entrypoint.sh"]
