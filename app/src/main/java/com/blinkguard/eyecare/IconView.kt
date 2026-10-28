package com.blinkguard.eyecare

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Single-colour icon drawn with Canvas paths, so it looks identical on every
 * device. Unicode glyphs render as emoji or tiny symbols on some phones.
 * Kinds: home, gear, info, sun, moon, diamond, eye, half, clock, heart, drop, warn, bolt, check
 */
class IconView @JvmOverloads constructor(
    context: Context,
    private val kind: String,
    private var iconColor: Int,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setIconColor(color: Int) {
        iconColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f * 0.82f
        paint.color = iconColor
        when (kind) {
            "home" -> drawHome(canvas, cx, cy, r)
            "gear" -> drawGear(canvas, cx, cy, r)
            "info" -> drawInfo(canvas, cx, cy, r)
            "sun" -> drawSun(canvas, cx, cy, r)
            "moon" -> drawMoon(canvas, cx, cy, r)
            "diamond" -> drawDiamond(canvas, cx, cy, r)
            "eye" -> drawEye(canvas, cx, cy, r)
            "half" -> drawHalf(canvas, cx, cy, r)
            "clock" -> drawClock(canvas, cx, cy, r)
            "heart" -> drawHeart(canvas, cx, cy, r)
            "drop" -> drawDrop(canvas, cx, cy, r)
            "warn" -> drawWarn(canvas, cx, cy, r)
            "bolt" -> drawBolt(canvas, cx, cy, r)
            "check" -> drawCheck(canvas, cx, cy, r)
        }
    }

    private fun strokePaint(width: Float): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    private fun drawHome(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.22f)
        val roof = Path().apply {
            moveTo(cx - r, cy - r * 0.1f)
            lineTo(cx, cy - r)
            lineTo(cx + r, cy - r * 0.1f)
        }
        canvas.drawPath(roof, p)
        val body = Path().apply {
            moveTo(cx - r * 0.72f, cy - r * 0.05f)
            lineTo(cx - r * 0.72f, cy + r * 0.85f)
            lineTo(cx + r * 0.72f, cy + r * 0.85f)
            lineTo(cx + r * 0.72f, cy - r * 0.05f)
        }
        canvas.drawPath(body, p)
    }

    private fun drawGear(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        canvas.drawCircle(cx, cy, r * 0.55f, p)
        canvas.drawCircle(cx, cy, r * 0.22f, p)
        for (i in 0 until 8) {
            val a = i * Math.PI / 4
            val x1 = cx + cos(a).toFloat() * r * 0.68f
            val y1 = cy + sin(a).toFloat() * r * 0.68f
            val x2 = cx + cos(a).toFloat() * r
            val y2 = cy + sin(a).toFloat() * r
            canvas.drawLine(x1, y1, x2, y2, p)
        }
    }

    private fun drawInfo(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        canvas.drawCircle(cx, cy, r * 0.88f, p)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = iconColor }
        canvas.drawCircle(cx, cy - r * 0.42f, r * 0.10f, fill)
        canvas.drawLine(cx, cy - r * 0.08f, cx, cy + r * 0.55f, p)
    }

    private fun drawSun(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = iconColor }
        canvas.drawCircle(cx, cy, r * 0.42f, fill)
        val p = strokePaint(r * 0.20f)
        for (i in 0 until 8) {
            val a = i * Math.PI / 4
            val x1 = cx + cos(a).toFloat() * r * 0.62f
            val y1 = cy + sin(a).toFloat() * r * 0.62f
            val x2 = cx + cos(a).toFloat() * r
            val y2 = cy + sin(a).toFloat() * r
            canvas.drawLine(x1, y1, x2, y2, p)
        }
    }

    private fun drawMoon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path().apply {
            addCircle(cx, cy, r * 0.85f, Path.Direction.CW)
            addCircle(cx + r * 0.55f, cy - r * 0.25f, r * 0.70f, Path.Direction.CW)
            fillType = Path.FillType.EVEN_ODD
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawDiamond(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.72f, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r * 0.72f, cy)
            close()
        }
        canvas.drawPath(path, p)
    }

    private fun drawEye(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        canvas.drawOval(cx - r, cy - r * 0.55f, cx + r, cy + r * 0.55f, p)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = iconColor }
        canvas.drawCircle(cx, cy, r * 0.30f, fill)
        val pupil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(11, 14, 18) }
        canvas.drawCircle(cx, cy, r * 0.13f, pupil)
    }

    private fun drawHalf(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        canvas.drawCircle(cx, cy, r * 0.88f, p)
        val path = Path().apply {
            moveTo(cx, cy - r * 0.88f)
            arcTo(cx - r * 0.88f, cy - r * 0.88f, cx + r * 0.88f, cy + r * 0.88f, 270f, 180f, false)
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawClock(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        canvas.drawCircle(cx, cy, r * 0.88f, p)
        canvas.drawLine(cx, cy, cx, cy - r * 0.55f, p)
        canvas.drawLine(cx, cy, cx + r * 0.55f, cy, p)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path().apply {
            moveTo(cx, cy + r * 0.85f)
            cubicTo(cx - r * 1.35f, cy - r * 0.05f, cx - r * 0.75f, cy - r, cx, cy - r * 0.30f)
            cubicTo(cx + r * 0.75f, cy - r, cx + r * 1.35f, cy - r * 0.05f, cx, cy + r * 0.85f)
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawDrop(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path().apply {
            moveTo(cx, cy - r)
            cubicTo(cx + r * 0.75f, cy - r * 0.05f, cx + r * 0.85f, cy + r * 0.45f, cx, cy + r * 0.90f)
            cubicTo(cx - r * 0.85f, cy + r * 0.45f, cx - r * 0.75f, cy - r * 0.05f, cx, cy - r)
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawWarn(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.20f)
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.9f, cy + r * 0.8f)
            lineTo(cx - r * 0.9f, cy + r * 0.8f)
            close()
        }
        canvas.drawPath(path, p)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = iconColor }
        canvas.drawCircle(cx, cy + r * 0.48f, r * 0.09f, fill)
        canvas.drawLine(cx, cy - r * 0.35f, cx, cy + r * 0.15f, p)
    }

    private fun drawBolt(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path().apply {
            moveTo(cx + r * 0.25f, cy - r)
            lineTo(cx - r * 0.45f, cy + r * 0.15f)
            lineTo(cx - r * 0.02f, cy + r * 0.15f)
            lineTo(cx - r * 0.25f, cy + r)
            lineTo(cx + r * 0.45f, cy - r * 0.15f)
            lineTo(cx + r * 0.02f, cy - r * 0.15f)
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawCheck(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = strokePaint(r * 0.28f)
        val path = Path().apply {
            moveTo(cx - r * 0.8f, cy)
            lineTo(cx - r * 0.2f, cy + r * 0.6f)
            lineTo(cx + r * 0.85f, cy - r * 0.6f)
        }
        canvas.drawPath(path, p)
    }
}
