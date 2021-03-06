package io.youi.stream

import io.youi.ajax.AjaxRequest
import io.youi.net.URL
import org.scalajs.dom.FormData

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object StreamURL {
  def stream(url: URL,
             data: Option[FormData] = None,
             timeout: Int = 0,
             headers: Map[String, String] = Map.empty,
             withCredentials: Boolean = true,
             responseType: String = ""): Future[String] = {
    val request = new AjaxRequest(url, data, timeout, headers + ("streaming" -> "true"), withCredentials, responseType)
    val future = request.send()
    future.map(_.responseText)
  }
}