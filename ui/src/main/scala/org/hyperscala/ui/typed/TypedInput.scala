package org.hyperscala.ui.typed

import org.hyperscala.html._
import org.powerscala.property.Property
import org.hyperscala.realtime.RealtimeEvent

/**
 * TypedInput is a simple wrapper class around an Input tag to bind the value of the input to a type.
 *
 * @author Matt Hicks <matt@outr.com>
 */
abstract class TypedInput[T](val wrapped: tag.Input)(implicit manifest: Manifest[T]) {
  private val changing = new ThreadLocal[Boolean] {
    override def initialValue() = false
  }

  val property = Property[Option[T]](default = None)
  property.change.on {
    case evt => updateInputFromProperty()   // Property has been modified, update the input
  }

  wrapped.keyUpEvent := RealtimeEvent(fireChange = true, preventDefault = false)
  wrapped.changeEvent := RealtimeEvent()
  wrapped.value.change.on {
    case evt => updatePropertyFromInput()   // Input value has been modified, update the property
  }

  updatePropertyFromInput()

  def toType(s: String): Option[T]
  def fromType(value: Option[T]): String

  def updatePropertyFromInput() = attemptChange {
    property := toType(wrapped.value())
  }

  def updateInputFromProperty() = attemptChange {
    wrapped.value := fromType(property())
  }

  private def attemptChange(f: => Unit) = if (!changing.get()) {
    changing.set(true)
    try {
      f
    } finally {
      changing.set(false)
    }
  }
}