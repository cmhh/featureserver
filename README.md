This repository contains a basic web service which can be used to download feature classes from a PostGIS database over HTTP.  It isn't intended to be particularly robust, but rather just illustrate the general concept.

The service uses the `ogr2ogr` command to convert PostGIS features to one of geopackage, geojson, or shapefile, zips the result, and streams it using Akka HTTP.  Note that Java bindings exist for GDAL, but it's much easier just to run ogr2ogr as an external process (there is an example Java program available which rougly replicates the C++ program, but it's well over 1000 lines long, and I didn't have the energy to look at it!).

To run the service, then, one requires a PostGIS database be running, and the `ogr2ogr` binary must be on the search path.  `sbt` is required to build the binary.  A fat jar can be made by running:

```bash
sbt assembly
```

The binary can be run as follows:

```bash
java \
  -Dpostgis.host=localhost -Dpostgis.db=gis -Dpostgis.port=5432 \
  -Dpostgis.user=username -Dpostgis.password=password \
  -cp featureserver.jar \
  org.cmhh.featureserver.Service
```

The repository also contains a containerised setup for testing.  Assuming `sbt assembly` has been run, the service can be started by running:

```bash
docker-compose up -d
```

* Note that this includes geopackage files the `./data` folder which requires [`lfs`](https://git-lfs.github.com/).  That is, clone using `git lfs clone`, rather than `git clone`. 

The service will then be available on localhost at port 9002.  There are just two endpoints: `/listFeatures` will list the available features, and `/getFeatureClass` can be used to download a specific feature.  For example:

```bash
$ curl -s localhost:9002/featureserver/listFeatures | jq
```
```json
[
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "regional_council_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "maori_constituency_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "constituency_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "maori_ward_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "territorial_authority_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "territorial_authority_local_board_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "subdivision_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "community_board_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "ward_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "urbanrural_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "statistical_area_1_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "statistical_area_2_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  },
  {
    "table_catalog": "gis",
    "table_schema": "statsnz",
    "table_name": "meshblock_2022",
    "geometry_column": "geom",
    "srid": 2193,
    "geom_type": "MULTIPOLYGON"
  }
]
```

To download `regional_council_2022` as a geopackage, we would then GET:

```plaintext
http://localhost:9002/featureserver/getFeatureClass?catalog=gis&schema=statsnz&name=regional_council_2022
```

The downloaded file will be called `regional_council_2022.gpkg.zip` by default, in this case.  To reproject the data, we can add a query parameter, `epgsg`; and to simplify, `simplify`.  For example, to output a web Mercator file, simplified to 100m (the feature class is stored in PostGIS in this case as EPSG:2193, which is in metres):

```plaintext
http://localhost:9002/featureserver/getFeatureClass?catalog=gis&schema=statsnz&name=regional_council_2022&epsg=4326&simplify=100
```