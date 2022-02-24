package org.cmhh.featureserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.ContentDispositionTypes.attachment
import akka.http.scaladsl.server.{ Route, Directive0 }
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.scaladsl.FileIO
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

/**
 * CORS handler... just in case.
 */
trait CORSHandler{
  private val corsResponseHeaders = List(
    headers.`Access-Control-Allow-Origin`.*,
    headers.`Access-Control-Allow-Credentials`(true),
    headers.`Access-Control-Allow-Headers`(
      "Authorization", "Content-Type", "X-Requested-With"
    )
  )
  
  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(corsResponseHeaders)
  }
  
  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).
      withHeaders(`Access-Control-Allow-Methods`(GET)))
  }
  
  def corsHandler(r: Route): Route = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }
  
  def addCORSHeaders(response: HttpResponse):HttpResponse =
    response.withHeaders(corsResponseHeaders)
}

/**
 * Entrypoint / service.
 */
object Service extends App with CORSHandler {  
  implicit val system = ActorSystem("featureserver")
  implicit val executionContext = system.dispatcher

  object routes {
    val listFeatures = path("listFeatures") {
      utils.geometryColumns match {
        case Success(features) =>
          complete(HttpEntity(
            ContentTypes.`application/json`,
            "[" + features.map(_.toString).mkString(",") + "]"
          ))
        case Failure(e) => 
          complete(HttpResponse(
            StatusCodes.InternalServerError, 
            entity = HttpEntity(ContentTypes.`application/json`, """"Failed to list features."""")
          ))
      }
    }

    val getFeatureClass = path("getFeatureClass") {
      parameters(
        "catalog", "schema", "name", "format".withDefault("gpkg"), "epsg".as[Int].?, "simplify".as[Double].?
      ){ (catalog, schema, name, format, epsg, simplify) =>
        val `application/geopackage+sqlite3` = MediaType.customBinary(
          "application", "geopackage+sqlite3", MediaType.Compressible
        )
        utils.exportAndZip(catalog, schema, name, format, epsg, simplify) match {
          case NonEmpty(res) =>
            val d = new java.io.File(res._1)
            val f = new java.io.File(res._2)
            val source = FileIO
              .fromPath(f.toPath)
              .watchTermination() { case (_, result) => 
                result.onComplete(_ => {
                  f.delete()
                  d.delete()
                })
              }
            respondWithHeader(`Content-Disposition`(attachment, Map("filename" -> f.getName()))) {
              complete(HttpEntity(ContentTypes.`application/octet-stream`, source))
            }
          case Empty =>
            complete(HttpResponse(
              StatusCodes.NoContent
            ))
          case Error(e) =>
            complete(HttpResponse(
              StatusCodes.InternalServerError, 
              entity = HttpEntity(ContentTypes.`application/json`, s""""${e.getMessage()}"""")
            ))
        }
      }
    }
  }

  val route = 
    pathPrefix("featureserver") { 
      corsHandler(
        get {
          routes.listFeatures ~ routes.getFeatureClass
        })
    }

  val bindingFuture = Http().newServerAt("0.0.0.0", 9001).bindFlow(route)

  println(s"Server online at http://localhost:9001/featureserver\nPress ENTER to stop...")
  StdIn.readLine() 
  bindingFuture
    .flatMap(_.unbind()) 
    .onComplete(_ => system.terminate()) 
}