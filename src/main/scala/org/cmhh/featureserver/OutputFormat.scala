package org.cmhh.featureserver

/**
 * Simple output format type.
 */
object OutputFormat {
  sealed trait Format
  object GPKG extends Format { override val toString = "GPKG" }
  object GEOJSON extends Format { override val toString = "GeoJSON" }
  object SHAPEFILE extends Format { override val toString = "ESRI Shapefile" }

  /**
   * Convert string to [[org.cmhh.featureserver.Format]]
   * 
   * Convert a string to an output format, with unsupported formats begin resolved as GPKG.
   * 
   * @param fmt String
   * @return [[org.cmhh.featureserver.Format]]
   */
  def formatFromString(fmt: String): Format = 
    if (List("GPKG").contains(fmt.toUpperCase)) GPKG
    else if (List("GEOJSON").contains(fmt.toUpperCase)) GEOJSON
    else if (List("SHP", "SHAPEFILE", "ESRI SHAPEFILE").contains(fmt.toUpperCase)) SHAPEFILE
    else GPKG
}