package io.youi.component.draw.path

import org.scalajs.dom.raw.CanvasRenderingContext2D

trait PathAction {
  def invoke(context: CanvasRenderingContext2D): Unit
}
