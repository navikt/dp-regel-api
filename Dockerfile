FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=true \
    APPD_NAME=dp-regel-api

#COPY .appdynamics/*.xml /opt/appdynamics/ver4.5.10.25916/conf/

COPY build/libs/*.jar app.jar
