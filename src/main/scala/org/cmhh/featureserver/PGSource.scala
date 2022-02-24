package org.cmhh.featureserver

/**
 * Representation of a PostGIS feature class.
 */
case class PGSource(
  tableCatalog: String, tableSchema: String, tableName: String, 
  geometryColumn: String, srid: Int, geomType: String
) {
  override def toString: String = {
    s"""{"table_catalog":"$tableCatalog","table_schema":"$tableSchema",""" +
      s""""table_name":"$tableName","geometry_column":"$geometryColumn","srid":$srid,""" +  
      s""""geom_type":"$geomType"}"""
  }
}