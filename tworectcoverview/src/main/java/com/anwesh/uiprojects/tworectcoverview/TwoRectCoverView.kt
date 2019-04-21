package com.anwesh.uiprojects.tworectcoverview

/**
 * Created by anweshmishra on 21/04/19.
 */

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.content.Context
import android.app.Activity
import android.view.View
import android.view.MotionEvent

val nodes : Int = 5
val rects : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")
val angleDeg : Float = 90f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateScale(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap
fun Int.sjf() : Float = 1f - 2 * (this)

fun Canvas.drawRotRect(x : Float, size : Float, sc1 : Float, sc2 : Float, paint : Paint) {
    drawRect(RectF(0f, -size/2, x * sc2, size / 2), paint)
    save()
    translate(x , 0f)
    rotate(angleDeg * sc1)
    drawRect(RectF(-size/2, -size, size/2, size), paint)
    restore()
}

fun Canvas.drawTRCNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val xOffset : Float = (w / 2 - size)
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(rects - 1)) {
        save()
        scale(j.sjf(), 1f)
        drawRotRect(xOffset, size, sc1.divideScale(j, rects), sc2.divideScale(j, rects), paint)
        restore()
    }
    restore()
}

class TwoRectCoverView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateScale(dir, rects, rects)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class TRCNode(var i : Int, val state : State = State()) {

        private var prev : TRCNode? = null
        private var next : TRCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = TRCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawTRCNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : TRCNode {
            var curr : TRCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class TwoRectCover(var i : Int) {

        private val root : TRCNode = TRCNode(0)
        private var curr : TRCNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : TwoRectCoverView) {

        private val trc : TwoRectCover = TwoRectCover(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            trc.draw(canvas, paint)
            animator.animate {
                trc.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            trc.startUpdating {
                animator.start()
            }
        }
    }
}