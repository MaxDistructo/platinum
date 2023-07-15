FROM eclipse-temurin:17-jdk-jammy
#WORKDIR /opt/platinum
RUN echo "1.1" > version
RUN apt update
RUN apt install git tesseract-ocr -y
#Below commands copy and build the repo
#RUN git clone https://github.com/MaxDistructo/platinum
#WORKDIR /opt/platinum/platinum
#RUN git checkout main
#RUN chmod +x gradlew
#RUN ./gradlew build
#WORKDIR /opt/platinum
RUN cp ./build/libs/platinum.jar /opt/platinum/platinum.jar
