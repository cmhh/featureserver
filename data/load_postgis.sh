#! /usr/bin/env bash
sleep 5

printf "Creating statsnz schema...\n"
PGPASSWORD=gisuser psql -U gisuser -d gis -c 'create schema statsnz;'
cd /data

printf "Loading regional council 2022...\n"
ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  regionalcouncil2022.gpkg \
  -nln statsnz.regional_council_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  maoriconstituency2022.gpkg \
  -nln statsnz.maori_constituency_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  constituency2022.gpkg \
  -nln statsnz.constituency_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  maoriward2022.gpkg \
  -nln statsnz.maori_ward_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  ta2022.gpkg \
  -nln statsnz.territorial_authority_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  talb2022.gpkg \
  -nln statsnz.territorial_authority_local_board_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  subdivision2022.gpkg \
  -nln statsnz.subdivision_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  communityboard2022.gpkg \
  -nln statsnz.community_board_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  ward2022.gpkg \
  -nln statsnz.ward_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  urbanrural2022.gpkg \
  -nln statsnz.urbanrural_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  statisticalarea12022.gpkg \
  -nln statsnz.statistical_area_1_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  statisticalarea22022.gpkg \
  -nln statsnz.statistical_area_2_2022

ogr2ogr \
  -f PostgreSQL PG:"dbname='gis' user='gisuser' password='gisuser'" \
  meshblock2022.gpkg \
  -nln statsnz.meshblock_2022
