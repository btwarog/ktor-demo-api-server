# Stage 1: Build the application
FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

# Stage 2: Create the final image
FROM openjdk:17
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-demo-api-server.jar
# Declare environment variables so Portainer can inject values at runtime
ENV DATABASE_URL=$DATABASE_URL
ENV DATABASE_USER=$DATABASE_USER
ENV DATABASE_PASSWORD=$DATABASE_PASSWORD
ENV JWT_SECRET=$JWT_SECRET
ENV JWT_ISSUER=$JWT_ISSUER
ENV JWT_AUDIENCE=$JWT_AUDIENCE
ENV UPLOAD_DIR=$UPLOAD_DIR

# Use exec form ENTRYPOINT to ensure environment variables are passed
ENTRYPOINT ["java", "-jar", "/app/ktor-demo-api-server.jar"]