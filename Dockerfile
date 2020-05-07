#FROM openjdk:11
FROM openjdk:8

WORKDIR /app/

COPY target/weka-server-1.0-SNAPSHOT-jar-with-dependencies.jar /app/server.jar

COPY config/ /root/config/
COPY models/ /root/models/
COPY input_data/iris.json /app/input_data/iris.json

# Install packages from Zip - avoids HTTPS proxy
#COPY weka-packages/ /app/packages/
# RUN java -cp server.jar weka.core.WekaPackageManager -install-package /app/packages/SMOTE1.0.3.zip

CMD java -jar server.jar
