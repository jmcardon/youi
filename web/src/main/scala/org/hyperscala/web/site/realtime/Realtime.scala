package org.hyperscala.web.site.realtime

import org.hyperscala.web.module.{jQuery, IdentifyTags, jQuery182, Module}
import org.hyperscala.web.site.{JavaScriptMessage, WebpageConnection, Webpage, Website}

import org.hyperscala.html._
import org.hyperscala.javascript.JavaScriptContent
import java.util.UUID
import annotation.tailrec
import org.powerscala.Version

import org.powerscala.json._

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Realtime extends Module {
  def name = "realtime"

  def version = Version(1)

  def load(page: Webpage) = {
    // Module requirements
    page.require(IdentifyTags)  // Make sure that every element has an id
    page.require(jQuery, jQuery182)   // jQuery is necessary

    // Configure JavaScript on page
    Website().register("/js/communicator.js", "communicator.js")
    Website().register("/js/realtime.js", "realtime.js")
    page.head.contents += new tag.Script(src = "/js/communicator.js")
    page.head.contents += new tag.Script(src = "/js/realtime.js")
    page.head.contents += new tag.Script(content = new JavaScriptContent {
      def content = {   // Every page request we create a new connection
        val id = UUID.randomUUID()
        val connection = Website().create(id)
        connection.page = page
        Realtime.addConnection(page, connection)
        "connectRealtime('%s');".format(id.toString)
      }

      protected def content_=(content: String) {}
    })
  }

  private val connectionsKey = "webpageConnections"

  def getConnections(page: Webpage) = synchronized {
    val connections = page.store.getOrElse[List[WebpageConnection]](connectionsKey, Nil)
    val updated = connections.filterNot(c => c.disposed)
    if (updated != connections) {
      page.store(connectionsKey) = updated
    }
    connections
  }

  def addConnection(page: Webpage, connection: WebpageConnection) = synchronized {
    val connections = page.store.getOrElse[List[WebpageConnection]](connectionsKey, Nil).filterNot(c => c.disposed)
    page.store(connectionsKey) = connection :: connections
  }

  def broadcast(event: String, message: Any, page: Webpage = Webpage()) = synchronized {
    val connections = getConnections(page)
    val content = message match {
      case s: String => s
      case other => generate(other)
    }
    sendRecursive(page, event, content, connections)
  }

  def sendJavaScript(instruction: String, content: String = null) = {
    broadcast("eval", JavaScriptMessage(instruction, content))
  }

  @tailrec
  private def sendRecursive(page: Webpage, event: String, message: String, connections: List[WebpageConnection]): Unit = {
    if (connections.nonEmpty) {
      val c = connections.head
      c.send(event, message)
      sendRecursive(page, event, message, connections.tail)
    }
  }
}