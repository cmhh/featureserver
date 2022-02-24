package org.cmhh.featureserver

import java.sql.{DriverManager, ResultSet}
import java.nio.file.Files
import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import com.typesafe.config.{Config, ConfigFactory}
import scala.util.{Try, Success, Failure}
import sys.process._

object db {
  import OutputFormat._

  val conf = ConfigFactory.load()
  private val host = conf.getString("postgis.host")
  private val user = conf.getString("postgis.user")
  private val password = conf.getString("postgis.password")
  private val port = conf.getString("postgis.port")
  private val db = conf.getString("postgis.db")
  private val url = s"jdbc:postgresql://$host:$port/$db"
  Class.forName("org.postgresql.Driver")
  private val con = DriverManager.getConnection(url, user, password) 

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

  /* make a command config object with sensible defaults?? */
  def command(
    table: String, path: String, format: Option[Format], epsg: Option[Int], simplify: Option[Double]
  ): Try[String] = Try {
    geometryColumns match {
      case Failure(e) => throw new Exception("Failed to retrieve feature list.")
      case Success(sources) => {
        val source = sources.filter(_.tableName.toLowerCase == table.toLowerCase).head

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

  def command(
    table: String, path: String, format: String, epsg: Option[Int], simplify: Option[Double]
  ): Try[String] = 
    command(table, path, Some(formatFromString(format)), epsg, simplify)

  def `export`(
    table: String, format: String, epsg: Option[Int], simplify: Option[Double]
  ): Try[String] = Try {
    val wd = Files.createTempDirectory("").toString
    command(table, wd, format, epsg, simplify) match {
      case Success(cmd) =>   
        val p = Process(s"""$cmd""")
        val res = p.!!
        wd
      case _ => 
        throw new Exception("Error creating command.")
    }
  }
  
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

  def exportAndZip(
    table: String, format: String, epsg: Option[Int], simplify: Option[Double]
  ): Try[(String, String)] = Try {
    `export`(table, format, epsg, simplify) match {
      case Success(d) =>
        zip(d, s"$d/$table.$format.zip") match {
          case Success(f) => (d, f)
          case Failure(e) => throw new Exception("Failed to create archive.")
        }
      case Failure(e) => throw new Exception("Failed to export feature class.")
    }
  }
}