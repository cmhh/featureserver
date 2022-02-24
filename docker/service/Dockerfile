FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive
ENV SHELL=/bin/bash

RUN  apt-get update && apt-get -y dist-upgrade && \
  apt-get install -y --no-install-recommends \
    gdal-bin libgdal-dev libgeos-dev libproj-dev libsqlite3-dev openjdk-11-jre-headless && \
  rm -rf /var/lib/apt/lists/* 

COPY target/scala-2.13/featureserver.jar featureserver.jar

EXPOSE 9001

