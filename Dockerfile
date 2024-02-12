FROM --platform=$BUILDPLATFORM gradle:8.6.0 as builder
COPY . /platinum
WORKDIR /platinum
RUN gradle build
#RUN cp ./build/libs/platinum.jar .

FROM eclipse-temurin:17.0.10_7-jre-jammy
#RUN echo "1.1" > version
COPY --from=builder /platinum/build/libs/platinum.jar /opt/platinum/platinum.jar
RUN apt update
RUN apt install tesseract-ocr -y