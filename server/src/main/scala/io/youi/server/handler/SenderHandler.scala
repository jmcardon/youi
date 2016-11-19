package io.youi.server.handler

import io.youi.http.{CacheControl, Content, Headers, HttpConnection, HttpRequest, HttpResponse, Status}
import io.youi.net.ContentType

class SenderHandler private(content: Content, contentType: ContentType, length: Option[Long], caching: CachingManager) extends HttpHandler {
  override def handle(connection: HttpConnection): Unit = {
    SenderHandler.handle(connection, content, contentType, length, caching)
  }
}

object SenderHandler {
  def apply(content: Content, contentType: ContentType, length: Option[Long] = None, caching: CachingManager = CachingManager.Default): SenderHandler = {
    new SenderHandler(content, contentType, length, caching)
  }

  def handle(connection: HttpConnection, content: Content, contentType: ContentType, length: Option[Long] = None, caching: CachingManager = CachingManager.Default): Unit = {
    val contentLength = length.getOrElse(content.length)
    connection.update { response =>
      response
        .withHeader(Headers.`Content-Type`(contentType))
        .withHeader(Headers.`Content-Length`(contentLength))
        .withContent(content)
    }
    caching.handle(connection)
  }
}

sealed trait CachingManager extends HttpHandler

object CachingManager {
  case object Default extends CachingManager {
    override def handle(connection: HttpConnection): Unit = {}
  }
  case object NotCached extends CachingManager {
    override def handle(connection: HttpConnection): Unit = {
      connection.update(_.withHeader(Headers.`Cache-Control`(CacheControl.NoCache, CacheControl.NoStore)))
    }
  }
  case class LastModified(publicCache: Boolean = true) extends CachingManager {
    val visibility = if (publicCache) CacheControl.Public else CacheControl.Private

    override def handle(connection: HttpConnection): Unit = connection.update { response =>
      response.content match {
        case Some(content) => {
          val ifModifiedSince = Headers.Request.`If-Modified-Since`.value(connection.request.headers).getOrElse(0L)
          if (ifModifiedSince == content.lastModified) {
            println("Not modified!")
            response.copy(status = Status.NotModified, headers = Headers.empty, content = None)
          } else {
            println(s"Modified: $ifModifiedSince")
            response
              .withHeader(Headers.`Cache-Control`(visibility))
              .withHeader(Headers.Response.`Last-Modified`(content.lastModified))
          }
        }
        case None => response
      }
    }
  }
  case class MaxAge(seconds: Long) extends CachingManager {
    override def handle(connection: HttpConnection): Unit = {
      connection.update(_.withHeader(Headers.`Cache-Control`(CacheControl.MaxAge(seconds))))
    }
  }
}