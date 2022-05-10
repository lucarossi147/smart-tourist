FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM openjdk:11
RUN mkdir /app
RUN mkdir /cert
COPY --from=build /home/gradle/src/build/libs/*.jar /app/
COPY --from=build /home/gradle/src/cert/*.jks /cert/

EXPOSE 443

ENTRYPOINT ["java","-jar","/app/ktor-auth-ms-0.0.1-all.jar"]