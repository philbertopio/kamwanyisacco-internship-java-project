# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Deploy WAR to Tomcat 9
FROM tomcat:9.0-jdk17-temurin
WORKDIR /usr/local/tomcat/webapps

RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/KimwanyiSacco.war ./ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]