package org.cmhh.featureserver

import java.sql.{DriverManager, ResultSet}
import java.nio.file.Files
import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import com.typesafe.config.{Config, ConfigFactory}
import scala.util.{Try, Success, Failure}
import sys.process._

/**
 * Utilities for accessing PostGIS data, and saving on disk.
 */
object utils {
  import OutputFormat._

  private val conf = ConfigFactory.load()
  private val host = conf.getString("postgis.host")
  private val user = conf.getString("postgis.user")
  private val password = conf.getString("postgis.password")
  private val port = conf.getString("postgis.port")
  private val db = conf.getString("postgis.db")
  private val url = s"jdbc:postgresql://$host:$port/$db"
  Class.forName("org.postgresql.Driver")
  private val con = DriverManager.getConnection(url, user, password) 

  /**
   * List feature classes in PostGIS database
   * 
   * @return A vector containing valid features.
   */
  def geometryColumns: Try[Vector[PGSource]] = Try {
    val s = con.createStatement()
    val q = "select * from geometry_columns"
    val r = s.executeQuery(q)
    val metaData = {
      val m = r.getMetaData()
      (1 to m.getColumnCount()).map(i => (m.getColumnName(i) -> i)).toMap
    }

    def loop(rs: ResultSet, accum: Vector[PGSource]): Vector[PGSource] = {
      if (!rs.next()) accum
      else {
        val source = PGSource(
          r.getString(metaData("f_table_catalog")),
          r.getString(metaData("f_table_schema")),
          r.getString(metaData("f_table_name")),
          r.getString(metaData("f_geometry_column")),
          r.getInt(metaData("srid")),
          r.getString(metaData("type"))
        )

        loop(rs, accum :+ source)
      }
    }

    val res = loop(r, Vector.empty)
    r.close()
    s.closeOnCompletion()

    res
  }

  /**
   * Generate ogr2ogr command
   * 
   * @param catalog database
   * @param schema schema
   * @param table feature class table
   * @param path output path
   * @param format output format (gpkg, geojson, or shapefile)
   * @param epsg output EPSG
   * @param simplify simplification level in native units
   * @return [[org.cmhh.featureserver.Result]] containing an ogr2ogr command
   */
  def command(
    catalog: String, schema: String, table: String, 
    path: String, format: Option[Format], epsg: Option[Int], simplify: Option[Double]
  ): Result[String] = {
    geometryColumns match {
      case Failure(e) => Error(e)
      case Success(sources) => {
        val candidates = sources.filter(x => {
          x.tableCatalog.toLowerCase == catalog.toLowerCase &
          x.tableSchema.toLowerCase == schema.toLowerCase &
          x.tableName.toLowerCase == table.toLowerCase 
        })

        if (candidates.size == 0) Empty else NonEmpty {
          val source = candidates.head

          val fmt = format match {
            case Some(f) => f
            case _ => GPKG 
          }

          val extStr = fmt match {
            case GPKG => "gpkg"
            case GEOJSON => "geojson"
            case SHAPEFILE => "shp"
          }

          val epsgStr = epsg match {
            case Some(x) => s"""-t_srs "EPSG:$x""""
            case _ => ""
          }

          val simplifyStr = simplify match {
            case Some(x) => s"""-simplify $x"""
            case _ => ""
          }

          val constr = s"""PG:"host=$host port=$port user=$user password=$password dbname=$db""""

          s"""ogr2ogr -f "$fmt" "$path/$table.$extStr" $constr "${source.tableSchema}.$table" """ +
            s"""${epsgStr} ${simplifyStr} -nlt ${source.geomType} -nln "$table" -overwrite"""
        }
      }
    }
  }

  /**
   * Generate ogr2ogr command
   * 
   * @param catalog database
   * @param schema schema
   * @param table feature class table
   * @param path output path
   * @param format output format (gpkg, geojson, or shapefile)
   * @param epsg output EPSG
   * @param simplify simplification level in native units
   * @return [[org.cmhh.featureserver.Result]] containing an ogr2ogr command
   */
  def command(
    catalog: String, schema: String, table: String,
    path: String, format: String, epsg: Option[Int], simplify: Option[Double]
  ): Result[String] = 
    command(catalog, schema, table, path, Some(formatFromString(format)), epsg, simplify)

  /**
   * Export feature class to disk
   * 
   * @param catalog database
   * @param schema schema
   * @param table feature class table
   * @param format output format (gpkg, geojson, or shapefile)
   * @param epsg output EPSG
   * @param simplify simplification level in native units
   * @return [[org.cmhh.featureserver.Result]] containing name of folder holding result
   */
  def `export`(
    catalog: String, schema: String, table: String, 
    format: String, epsg: Option[Int], simplify: Option[Double]
  ): Result[String] = {
    val wd = Files.createTempDirectory("").toFile
    command(catalog, schema, table, wd.toString, format, epsg, simplify) match {
      case NonEmpty(cmd) =>  
        val p = Process(s"""$cmd""")
        val res = p.!!
        if (wd.listFiles.isEmpty) 
          Error(new Exception("Command failed."))
        else 
          NonEmpty(wd.toString)
      case Empty => Empty
      case Error(e) => Error(e)
    }
  }
  
  /**
   * Compress contents of folder
   * 
   * @param folder parent folder
   * @param outfile output file name
   * @return name of output file
   */
  def zip(folder: String, outfile: String): Try[String] = Try {
    val dir: File = new File(folder)
    val files = dir.listFiles().toList
    val fos: FileOutputStream = new FileOutputStream(outfile)
    val zos: ZipOutputStream = new ZipOutputStream(fos)
    files.foreach(f => {
      val fis = new FileInputStream(f)
      val entry = new ZipEntry(f.getName())
      zos.putNextEntry(entry)
      val buff = Array.fill[Byte](1024)(0)
      def loop(s: FileInputStream): Unit = {
        val n = s.read(buff)
        if (n < 0) ()
        else {
          zos.write(buff, 0, n)
          loop(s)
        }
      }
      loop(fis)
      fis.close()
      f.delete()
    })

    zos.close()
    fos.close()

    outfile
  }

  /**
   * Export feature class to compressed archive
   * 
   * @param catalog database
   * @param schema schema
   * @param table feature class table
   * @param format output format (gpkg, geojson, or shapefile)
   * @param epsg output EPSG
   * @param simplify simplification level in native units
   * @return [[org.cmhh.featureserver.Result]] containing names of folder and compressed file
   */
  def exportAndZip(
    catalog: String, schema: String, table: String,
    format: String, epsg: Option[Int], simplify: Option[Double]
  ): Result[(String, String)] = {
    `export`(catalog, schema, table, format, epsg, simplify) match {
      case NonEmpty(d) =>
        zip(d, s"$d/$table.$format.zip") match {
          case Success(f) => NonEmpty((d, f))
          case Failure(e) => Error(throw new Exception("Failed to create archive."))
        }
      case Empty => Empty
      case Error(e) => Error(e)
    }
  }
}