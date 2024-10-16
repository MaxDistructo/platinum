FROM --platform=$BUILDPLATFORM gradle:8.10.2-jdk21-alpine as builder
COPY . /platinum
WORKDIR /platinum
RUN gradle build
#RUN cp ./build/libs/platinum.jar .

FROM eclipse-temurin:21.0.4_7-jre-alpine
#RUN echo "1.1" > version
COPY --from=builder /platinum/build/libs/platinum.jar /opt/platinum/platinum.jar
RUN apk update && apk upgrade
RUN apk add tesseract-ocr
