FROM --platform=$BUILDPLATFORM gradle:8.9.0-jdk21 as builder
COPY . /platinum
WORKDIR /platinum
RUN gradle build
#RUN cp ./build/libs/platinum.jar .

FROM eclipse-temurin:21.0.4_7-jre-jammy
#RUN echo "1.1" > version
COPY --from=builder /platinum/build/libs/platinum.jar /opt/platinum/platinum.jar
RUN apt update
RUN apt install tesseract-ocr -y
