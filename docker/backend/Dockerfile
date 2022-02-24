FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive
ENV SHELL=/bin/bash

RUN  apt-get update && apt-get -y dist-upgrade && \
  apt-get install -y --no-install-recommends \
    wget gnupg2 ca-certificates gdal-bin sudo vim \
    libgdal-dev libgeos-dev libproj-dev libsqlite3-dev libudunits2-dev && \
  sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt focal-pgdg main" > /etc/apt/sources.list.d/pgdg.list' && \
  wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - && \
  apt-get update && apt-get install -y --no-install-recommends postgresql-14 postgresql-14-postgis-3 postgis && \
  sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
  dpkg-reconfigure --frontend=noninteractive locales && \
  update-locale LANG=en_US.UTF-8 && \
  apt-get remove -y gdebi && apt-get autoremove -y && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/* 

RUN service postgresql start && \
  sudo -u postgres psql -c 'create database gis;' && \
  sudo -u postgres psql -d gis -c 'create extension postgis;' && \
  sudo -u postgres psql -d gis -c 'create extension postgis_raster;' && \
  sudo -u postgres psql -d gis -c 'create extension postgis_sfcgal;' && \
  sudo -u postgres psql -d gis -c 'create extension postgis_topology;' && \
  sudo -u postgres psql -d gis -c "SET postgis.gdal_enabled_drivers = 'ENABLE_ALL';" && \
  sudo -u postgres psql -c 'create user gisuser;' && \
  sudo -u postgres psql -c "alter user gisuser with encrypted password 'gisuser';" && \
  sudo -u postgres psql -c 'grant all privileges on database gis to gisuser;' && \
  printf "\tlisten_addresses='*'\t" >> /etc/postgresql/14/main/postgresql.conf && \
  sed -i -E '/local +all +all +peer/ s/peer/md5/' /etc/postgresql/14/main/pg_hba.conf && \
  sed -i -E '/host +all +all +127.0.0.1\/32 +scram-sha-256/ s/127.0.0.1\/32/0.0.0.0\/0   /' /etc/postgresql/14/main/pg_hba.conf && \
  sed -i -E '/host +all +all +::1\/128 +scram-sha-256/ s/::1\/128/::0\/0  /' /etc/postgresql/14/main/pg_hba.conf &&\
  printf "localhost:5432:gis:gisuser:gisuser" >> /root/.pgpass && \
  chmod 0600 /root/.pgpass

RUN adduser --disabled-password --gecos "" gisuser && \
  usermod --password $(openssl passwd -1 gisuser) gisuser && \
  usermod -aG sudo gisuser

EXPOSE 5432

ENTRYPOINT service postgresql start && \
  sh /data/load_postgis.sh && \ 
  tail -f
