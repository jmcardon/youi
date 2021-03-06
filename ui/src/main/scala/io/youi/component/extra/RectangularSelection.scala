package io.youi.component.extra

import io.youi.component.draw.path.{Path, PathAction, Rectangle}
import io.youi.component.draw._
import io.youi.component.event.{DragSupport, Pointer}
import io.youi.component.{DrawableComponent, PaintSupport, PaintTheme}
import io.youi.style.{Cursor, Paint}
import io.youi.theme.RectangularSelectionTheme
import org.scalajs.dom.raw.CanvasRenderingContext2D
import reactify._

import scala.concurrent.Future

class RectangularSelection extends DrawableComponent {
  override lazy val theme: Var[_ <: RectangularSelectionTheme] = Var(RectangularSelection)

  object selection extends PaintSupport {
    val x1: Var[Double] = Var(0.0)
    val y1: Var[Double] = Var(0.0)
    val x2: Var[Double] = Var(0.0)
    val y2: Var[Double] = Var(0.0)
    val width: Val[Double] = Val(x2 - x1)
    val height: Val[Double] = Val(y2 - y1)
    val edgeDistance: Var[Double] = Var(5.0)
    object aspectRatio extends Var[Option[Double]](() => None) {
      def bySize(width: Double, height: Double): Unit = set(Some(width / height))
    }

    val minX: Var[Double] = Var(edgeDistance)
    val minY: Var[Double] = Var(edgeDistance)
    val maxX: Var[Double] = Var(size.width - edgeDistance)
    val maxY: Var[Double] = Var(size.height - edgeDistance)
    val minWidth: Var[Double] = Var(30.0)
    val minHeight: Var[Double] = Var(30.0)

    override protected def paintTheme: PaintTheme = theme.selection

    def set(x1: Double, y1: Double, x2: Double, y2: Double): Unit = {
      this.x1 := x1
      this.y1 := y1
      this.x2 := x2
      this.y2 := y2
    }
    def maximize(): Unit = {
      x1.static(minX)
      y1.static(minY)
      x2.static(maxX)
      y2.static(maxY)
    }
  }
  object blocks extends PaintSupport {
    val size: Var[Double] = Var(10.0)

    override protected def paintTheme: PaintTheme = theme.blocks
  }

  object dashes extends PaintSupport {
    object shadow {
      val enabled: Var[Boolean] = Var(theme.dashes.shadow.enabled)
      object offset {
        val x: Var[Double] = Var(theme.dashes.shadow.offset.x)
        val y: Var[Double] = Var(theme.dashes.shadow.offset.y)
      }
      val paint: Var[Paint] = Var(theme.dashes.shadow.paint)
      val lineWidth: Var[Double] = Var(theme.dashes.shadow.lineWidth)
    }

    override protected def paintTheme: PaintTheme = theme.dashes
  }

  lazy val overflow: Var[Paint] = Var(theme.overflow)

  object modal extends PaintSupport {
    override protected def paintTheme: PaintTheme = theme.modal
  }

  private val dragging = new SelectionDragSupport(this)
  def isDragging: Boolean = dragging.isDragging

  drawable := {
    if (selection.width() != 0.0 && selection.height() != 0.0) {
      Group(
        createModal(),
        createDashes(),
        createOverflow(),
        createSelection(),
        createBlocks()
      )
    } else {
      Drawable.empty
    }
  }

  cursor := cursorForMouse()

  def cursorForMouse(pointerX: Double = event.pointer.x(), pointerY: Double = event.pointer.y()): Cursor = {
    val ed = selection.edgeDistance()
    if (pointerX >= selection.x1 - ed && pointerX <= selection.x2 + ed && pointerY >= selection.y1 - ed && pointerY <= selection.y2 + ed) {
      if (near(selection.x1, pointerX)) {
        if (near(selection.y1, pointerY)) {
          Cursor.ResizeNorthWest
        } else if (near(selection.y2, pointerY)) {
          Cursor.ResizeSouthWest
        } else {
          Cursor.ResizeWest
        }
      } else if (near(selection.x2, pointerX)) {
        if (near(selection.y1, pointerY)) {
          Cursor.ResizeNorthEast
        } else if (near(selection.y2, pointerY)) {
          Cursor.ResizeSouthEast
        } else {
          Cursor.ResizeEast
        }
      } else if (near(selection.y1, pointerY)) {
        Cursor.ResizeNorth
      } else if (near(selection.y2, pointerY)) {
        Cursor.ResizeSouth
      } else {
        Cursor.Move
      }
    } else {
      Cursor.Default
    }
  }

  protected def createSelection(): Drawable = Group(
    Path
      .begin
      .rect(selection.x1(), selection.y1(), selection.width(), selection.height())
      .fix()
      .close,
    selection.fill.value,
    selection.stroke.value
  )

  protected def createOverflow(): Drawable = if (overflow().nonEmpty) {
    val halfBlock = blocks.size() / 2.0
    Group(
      Path
        .begin
        .rect(0.0, 0.0, halfBlock, size.height())                                             // Left
        .rect(halfBlock, 0.0, size.width() - blocks.size(), halfBlock)                        // Top
        .rect(size.width() - halfBlock, 0.0, halfBlock, size.height())                        // Right
        .rect(halfBlock, size.height() - halfBlock, size.width() - blocks.size(), halfBlock)  // Bottom
        .fix(),
      Fill(overflow)
    )
  } else {
    Drawable.empty
  }

  protected def createBlocks(): Drawable = {
    val halfBlock = blocks.size() / 2.0
    def block(x: Double, y: Double): PathAction = {
      Rectangle(x - halfBlock, y - halfBlock, blocks.size(), blocks.size())
    }
    Group(
      Path
        .begin
        .withAction(block(selection.x1, selection.y1))
        .withAction(block(selection.x1 + (selection.width / 2.0), selection.y1))
        .withAction(block(selection.x2, selection.y1))
        .withAction(block(selection.x1, selection.y1 + (selection.height / 2.0)))
        .withAction(block(selection.x2, selection.y1 + (selection.height / 2.0)))
        .withAction(block(selection.x1, selection.y2))
        .withAction(block(selection.x1 + (selection.width / 2.0), selection.y2))
        .withAction(block(selection.x2, selection.y2))
        .close
        .fix(),
      blocks.fill.value,
      blocks.stroke.value
    )
  }
  protected def createModal(): Drawable = {
    val halfBlock = blocks.size() / 2.0
    Group(
      Path
        .begin
        .rect(halfBlock, halfBlock, selection.x1() - halfBlock, size.height() - blocks.size())                  // Left
        .rect(selection.x2(), halfBlock, size.width() - selection.x2() - halfBlock, size.height() - blocks.size())          // Right
        .rect(selection.x1(), halfBlock, selection.width(), selection.y1() - halfBlock)                         // Top
        .rect(selection.x1(), selection.y2(), selection.width(), size.height() - selection.y2() - halfBlock)    // Bottom
        .close
        .fix(),
      modal.fill.value,
      modal.stroke.value
    )
  }

  protected def createDashes(): Drawable = {
    val horizontalThird = selection.width() / 3.0
    val verticalThird = selection.height() / 3.0
    val group = if (dashes.shadow.enabled()) {
      Group(
        Path
          .begin
          // Horizontal Bar 1
          .move(selection.x1 + (selection.width / 2.0), selection.y1 + verticalThird)
          .line(selection.x1, selection.y1 + verticalThird)
          .move(selection.x1 + (selection.width / 2.0), selection.y1 + verticalThird)
          .line(selection.x2, selection.y1 + verticalThird)
          // Horizontal Bar 2
          .move(selection.x1 + (selection.width / 2.0), selection.y2 - verticalThird)
          .line(selection.x1, selection.y2 - verticalThird)
          .move(selection.x1 + (selection.width / 2.0), selection.y2 - verticalThird)
          .line(selection.x2, selection.y2 - verticalThird)
          // Vertical Bar 1
          .move(selection.x1 + horizontalThird, selection.y1 + (selection.height / 2.0))
          .line(selection.x1 + horizontalThird, selection.y1)
          .move(selection.x1 + horizontalThird, selection.y1 + (selection.height / 2.0))
          .line(selection.x1 + horizontalThird, selection.y2)
          // Vertical Bar 2
          .move(selection.x2 - horizontalThird, selection.y1 + (selection.height / 2.0))
          .line(selection.x2 - horizontalThird, selection.y1)
          .move(selection.x2 - horizontalThird, selection.y1 + (selection.height / 2.0))
          .line(selection.x2 - horizontalThird, selection.y2)
          .shift(dashes.shadow.offset.x, dashes.shadow.offset.y)
          .fix(),
        Stroke(dashes.shadow.paint, dashes.shadow.lineWidth, dashes.stroke.lineDash, dashes.stroke.lineDashOffset, dashes.stroke.lineCap, dashes.stroke.lineJoin)
      )
    } else {
      Group()
    }
    group.withDrawables(
      RestoreContext,
      Path
        .begin
        // Horizontal Bar 1
        .move(selection.x1 + (selection.width / 2.0), selection.y1 + verticalThird)
        .line(selection.x1, selection.y1 + verticalThird)
        .move(selection.x1 + (selection.width / 2.0), selection.y1 + verticalThird)
        .line(selection.x2, selection.y1 + verticalThird)
        // Horizontal Bar 2
        .move(selection.x1 + (selection.width / 2.0), selection.y2 - verticalThird)
        .line(selection.x1, selection.y2 - verticalThird)
        .move(selection.x1 + (selection.width / 2.0), selection.y2 - verticalThird)
        .line(selection.x2, selection.y2 - verticalThird)
        // Vertical Bar 1
        .move(selection.x1 + horizontalThird, selection.y1 + (selection.height / 2.0))
        .line(selection.x1 + horizontalThird, selection.y1)
        .move(selection.x1 + horizontalThird, selection.y1 + (selection.height / 2.0))
        .line(selection.x1 + horizontalThird, selection.y2)
        // Vertical Bar 2
        .move(selection.x2 - horizontalThird, selection.y1 + (selection.height / 2.0))
        .line(selection.x2 - horizontalThird, selection.y1)
        .move(selection.x2 - horizontalThird, selection.y1 + (selection.height / 2.0))
        .line(selection.x2 - horizontalThird, selection.y2)
        .fix(),
      dashes.fill.value,
      dashes.stroke.value
    )
  }

  private def near(from: Double, to: Double): Boolean = {
    math.abs(from - to) <= selection.edgeDistance()
  }

  override protected def draw(context: CanvasRenderingContext2D): Future[Unit] = {
    dragging.update()
    super.draw(context)
  }

  override protected def autoPaint = false
}

class SelectionDragSupport(rs: RectangularSelection) extends DragSupport[DragStart](rs) {
  override def draggable(pointer: Pointer): Option[DragStart] = {
    val mouseEvent = pointer.move
    val x1 = rs.selection.x1() - rs.selection.edgeDistance()
    val y1 = rs.selection.y1() - rs.selection.edgeDistance()
    val x2 = rs.selection.x2() + rs.selection.edgeDistance()
    val y2 = rs.selection.y2() + rs.selection.edgeDistance()
    if (mouseEvent.x >= x1 && mouseEvent.x <= x2 && mouseEvent.y >= y1 && mouseEvent.y <= y2) {
      val cursor = rs.cursorForMouse(mouseEvent.x, mouseEvent.y)
      Some(DragStart(cursor, rs.selection.x1(), rs.selection.y1(), rs.selection.x2(), rs.selection.y2(), mouseEvent.globalX, mouseEvent.globalY))
    } else {
      None
    }
  }

  override def dragging(pointer: Pointer, value: DragStart): Unit = {
    super.dragging(pointer, value)

    val mouseEvent = pointer.move
    val adjustX = mouseEvent.globalX - value.mouseX
    val adjustY = mouseEvent.globalY - value.mouseY
    def processCursor(cursor: Cursor): Unit = {
      cursor match {
        case Cursor.Move => {
          var x1 = value.x1 + adjustX
          var x2 = value.x2 + adjustX
          var y1 = value.y1 + adjustY
          var y2 = value.y2 + adjustY

          if (x1 < rs.selection.minX()) {
            val a = rs.selection.minX - x1
            x1 += a
            x2 += a
          } else if (x2 > rs.selection.maxX()) {
            val a = x2 - rs.selection.maxX
            x1 -= a
            x2 -= a
          }
          if (y1 < rs.selection.minY()) {
            val a = rs.selection.minY - y1
            y1 += a
            y2 += a
          } else if (y2 > rs.selection.maxY()) {
            val a = y2 - rs.selection.maxY
            y1 -= a
            y2 -= a
          }

          update(x1, y1, x2, y2)
        }
        case Cursor.ResizeWest => {
          var x1 = math.max(value.x1 + adjustX, rs.selection.minX)
          if (rs.selection.x2 - x1 < rs.selection.minWidth()) {
            x1 = rs.selection.x2 - rs.selection.minWidth
          }
          update(x1 = x1)
        }
        case Cursor.ResizeEast => {
          var x2 = math.min(value.x2 + adjustX, rs.selection.maxX)
          if (x2 - rs.selection.x1 < rs.selection.minWidth()) {
            x2 = rs.selection.x1 + rs.selection.minWidth
          }
          update(x2 = x2)
        }
        case Cursor.ResizeNorth => {
          var y1 = math.max(value.y1 + adjustY, rs.selection.minY)
          if (rs.selection.y2 - y1 < rs.selection.minHeight()) {
            y1 = rs.selection.y2 - rs.selection.minHeight
          }
          update(y1 = y1)
        }
        case Cursor.ResizeSouth => {
          var y2 = math.min(value.y2 + adjustY, rs.selection.maxY)
          if (y2 - rs.selection.y1 < rs.selection.minHeight()) {
            y2 = rs.selection.y1 + rs.selection.minHeight
          }
          update(y2 = y2)
        }
        case Cursor.ResizeNorthWest => {
          processCursor(Cursor.ResizeNorth)
          processCursor(Cursor.ResizeWest)
        }
        case Cursor.ResizeNorthEast => {
          processCursor(Cursor.ResizeNorth)
          processCursor(Cursor.ResizeEast)
        }
        case Cursor.ResizeSouthWest => {
          processCursor(Cursor.ResizeSouth)
          processCursor(Cursor.ResizeWest)
        }
        case Cursor.ResizeSouthEast => {
          processCursor(Cursor.ResizeSouth)
          processCursor(Cursor.ResizeEast)
        }
        case _ => scribe.debug(s"Ignoring $value")
      }
    }
    processCursor(value.cursor)
  }

  def update(x1: Double = rs.selection.x1,
             y1: Double = rs.selection.y1,
             x2: Double = rs.selection.x2,
             y2: Double = rs.selection.y2): Unit = {
    val w = x2 - x1
    val h = y2 - y1
    val aspectRatio = w / h

    rs.selection.aspectRatio() match {
      case Some(ar) if math.abs(ar - aspectRatio) > 0.001 => {    // Recalculate for aspect ratio
        val wd = math.abs(w - rs.selection.width)
        val hd = math.abs(h - rs.selection.height)
        val cursor = value().map(_.cursor).getOrElse(Cursor.Move)
        if (wd > hd || (wd == 0.0 && hd == 0.0 && ar > aspectRatio)) {      // Calculate based on width
          val newHeight = w / ar
          cursor match {
            case Cursor.ResizeNorth | Cursor.ResizeNorthEast | Cursor.ResizeNorthWest => {
              update(x1, y2 - newHeight, x2, y2)
            }
            case _ => {
              if (cursor == Cursor.Move) {
                val middle = (rs.selection.maxY + rs.selection.minY) / 2.0
                update(x1, middle - (newHeight / 2.0), x2, middle + (newHeight / 2.0))
              } else {
                update(x1, y1, x2, y1 + newHeight)
              }
            }
          }
        } else {            // Calculate based on height
          val newWidth = h * ar
          cursor match {
            case Cursor.ResizeWest | Cursor.ResizeNorthWest | Cursor.ResizeSouthWest => {
              update(x2 - newWidth, y1, x2, y2)
            }
            case _ => {
              if (cursor == Cursor.Move) {
                val center = (rs.selection.maxX  + rs.selection.minX) / 2.0
                update(center - (newWidth / 2.0), y1, center + (newWidth / 2.0), y2)
              } else {
                update(x1, y1, x1 + newWidth, y2)
              }
            }
          }
        }
      }
      case _ if x1 < rs.selection.minX() => // Ignore
      case _ if x2 > rs.selection.maxX() => // Ignore
      case _ if y1 < rs.selection.minY() => // Ignore
      case _ if y2 > rs.selection.maxY() => // Ignore
      case _ => {
        rs.selection.x1.static(x1)
        rs.selection.x2.static(x2)
        rs.selection.y1.static(y1)
        rs.selection.y2.static(y2)
      }
    }
  }
}

case class DragStart(cursor: Cursor, x1: Double, y1: Double, x2: Double, y2: Double, mouseX: Double, mouseY: Double)

object RectangularSelection extends RectangularSelectionTheme