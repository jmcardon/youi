package io.youi.example.ui

import io.youi._
import io.youi.app.screen.UIScreen
import io.youi.component.DrawableComponent
import io.youi.component.layout.Snap
import io.youi.style.Paint
import reactify._

object SnapExample extends UIExampleScreen with UIScreen {
  override def name: String = "Snap"
  override def path: String = "/examples/snap.html"

  override def createUI(): Unit = {
    val box = new Box {
      background := Color.Black
      position.center := container.position.center
      position.middle := container.position.middle
    }

    val blue = Box(Color.Blue)
    Snap(blue).rightTo(box.position.left - 5.0).topTo(box.position.top)

    val green = Box(Color.Green)
    Snap(green).leftTo(blue.position.left).rightTo(box.position.right).topTo(box.position.bottom + 5.0)

    val yellow = Box(Color.Yellow.withAlpha(0.5))
    Snap(yellow).leftTo(blue.position.center).rightTo(box.position.center).topTo(box.position.middle)

    container.children ++= List(
      box,
      blue,
      green,
      yellow
    )
  }

  class Box extends DrawableComponent {
    size.width := 100.0
    size.height := 100.0
  }

  object Box {
    def apply(paint: Paint): Box = new Box {
      background := paint
    }
  }
}