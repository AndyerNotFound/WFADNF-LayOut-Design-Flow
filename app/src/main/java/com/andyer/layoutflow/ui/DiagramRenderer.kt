package com.andyer.layoutflow.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.andyer.layoutflow.model.FlowEdge
import com.andyer.layoutflow.model.FlowNode
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.arrowHead
import com.andyer.layoutflow.model.bounds
import com.andyer.layoutflow.model.displayLabel
import com.andyer.layoutflow.model.edgeGeometry
import com.andyer.layoutflow.model.platformInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

private const val BG = 0xFFFEF7FF.toInt()
private const val SURFACE = 0xFFF7F2FA.toInt()
private const val SURFACE_HIGH = 0xFFECE6F0.toInt()
private const val PRIMARY = 0xFF6750A4.toInt()
private const val ON_PRIMARY = 0xFFFFFFFF.toInt()
private const val PRIMARY_CONTAINER = 0xFFEADDFF.toInt()
private const val ON_PRIMARY_CONTAINER = 0xFF21005D.toInt()
private const val SECONDARY_CONTAINER = 0xFFE8DEF8.toInt()
private const val TERTIARY_CONTAINER = 0xFFFFD8E4.toInt()
private const val OUTLINE = 0xFF79747E.toInt()
private const val OUTLINE_VARIANT = 0xFFCAC4D0.toInt()
private const val ON_SURFACE = 0xFF1D1B20.toInt()
private const val ON_SURFACE_VARIANT = 0xFF49454F.toInt()
private const val ERROR = 0xFFB3261E.toInt()
private const val DARK = 0xFF322F35.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

private val normalTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
private val mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
private val boldTypeface = Typeface.create("sans-serif", Typeface.BOLD)

private fun fill(color: Int, alpha: Int = 255): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    this.color = color
    this.alpha = alpha
}

private fun stroke(color: Int, width: Float = 1f, alpha: Int = 255): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    this.color = color
    strokeWidth = width
    this.alpha = alpha
}

private fun textPaint(size: Float, color: Int, bold: Boolean = false): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    textSize = size
    typeface = if (bold) boldTypeface else normalTypeface
}

private fun Canvas.text(
    value: String,
    x: Float,
    y: Float,
    size: Float,
    color: Int,
    bold: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT,
) {
    val paint = textPaint(size, color, bold)
    paint.textAlign = align
    drawText(value, x, y, paint)
}

private fun Canvas.line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 1f) {
    drawLine(x1, y1, x2, y2, stroke(color, width))
}

private fun Canvas.roundBox(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
    color: Int,
    strokeColor: Int? = null,
    strokeWidth: Float = 1f,
) {
    val rect = RectF(left, top, right, bottom)
    drawRoundRect(rect, radius, radius, fill(color))
    if (strokeColor != null) {
        drawRoundRect(rect, radius, radius, stroke(strokeColor, strokeWidth))
    }
}

private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
    if (value.isEmpty()) return value
    if (paint.measureText(value) <= maxWidth) return value
    val dots = "…"
    var end = value.length
    while (end > 1 && paint.measureText(value, 0, end) + paint.measureText(dots) > maxWidth) {
        end--
    }
    return value.substring(0, end) + dots
}

private fun Canvas.centeredEllipsis(value: String, cx: Float, cy: Float, maxWidth: Float, size: Float, color: Int, bold: Boolean = false) {
    val paint = textPaint(size, color, bold)
    paint.textAlign = Paint.Align.CENTER
    val shown = ellipsize(value, paint, maxWidth)
    drawText(shown, cx, cy + size * 0.36f, paint)
}

object DiagramRenderer {

    fun render(project: FlowProject, requestedScale: Float = 2f): Bitmap {
        val b = project.bounds()
        if (b == null) {
            val bitmap = Bitmap.createBitmap(900, 420, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(BG)
            canvas.text(project.name, 450f, 200f, 26f, ON_SURFACE, true, Paint.Align.CENTER)
            canvas.text("Empty flow", 450f, 232f, 14f, OUTLINE, false, Paint.Align.CENTER)
            return bitmap
        }
        val padding = 40f
        val header = 64f
        val worldWidth = b.width + padding * 2f
        val worldHeight = b.height + header + padding * 2f
        var scale = requestedScale.coerceIn(0.5f, 4f)
        val maxSide = 7000f
        if (worldWidth * scale > maxSide || worldHeight * scale > maxSide) {
            scale = min(maxSide / worldWidth, maxSide / worldHeight)
        }
        val width = ceil(worldWidth * scale).toInt().coerceAtLeast(480)
        val height = ceil(worldHeight * scale).toInt().coerceAtLeast(240)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BG)

        canvas.text(project.name, 24f * scale, 32f * scale, 21f * scale, ON_SURFACE, true)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.text(
            "LayOut Flow · ${project.nodes.size} 个元件 · ${project.edges.size} 条连线 · $stamp",
            24f * scale,
            52f * scale,
            10.5f * scale,
            ON_SURFACE_VARIANT,
        )

        canvas.save()
        canvas.translate((padding - b.left) * scale, (header + padding - b.top) * scale)
        canvas.scale(scale, scale)

        for (edge in project.edges) {
            drawEdge(canvas, project, edge)
        }
        for (node in project.nodes) {
            drawNode(canvas, node)
        }

        canvas.restore()
        return bitmap
    }

    private fun drawEdge(canvas: Canvas, project: FlowProject, edge: FlowEdge) {
        val g = edgeGeometry(project, edge) ?: return
        val path = Path().apply {
            moveTo(g.startX, g.startY)
            cubicTo(g.c1x, g.c1y, g.c2x, g.c2y, g.endX, g.endY)
        }
        canvas.drawPath(path, stroke(PRIMARY, 2.6f, 220))
        val head = arrowHead(g.c2x, g.c2y, g.endX, g.endY, 10f, 8f)
        val headPath = Path().apply {
            moveTo(head[0].first, head[0].second)
            lineTo(head[1].first, head[1].second)
            lineTo(head[2].first, head[2].second)
            close()
        }
        canvas.drawPath(headPath, fill(PRIMARY))
        canvas.drawCircle(g.startX, g.startY, 3.2f, fill(PRIMARY))
        canvas.drawCircle(g.startX, g.startY, 6.5f, stroke(PRIMARY, 1f, 90))

        val (mx, my) = g.pointAt(0.5f)
        val label = edge.displayLabel()
        val paint = textPaint(9.5f, ON_PRIMARY_CONTAINER)
        val maxWidth = 220f
        val shown = ellipsize(label, paint, maxWidth - 16f)
        val labelWidth = (paint.measureText(shown) + 18f).coerceAtMost(maxWidth)
        val rect = RectF(mx - labelWidth / 2f, my - 10f, mx + labelWidth / 2f, my + 10f)
        canvas.drawRoundRect(rect, 10f, 10f, fill(PRIMARY_CONTAINER, 245))
        canvas.drawRoundRect(rect, 10f, 10f, stroke(OUTLINE_VARIANT, 0.8f))
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(shown, mx, my + 3.4f, paint)
    }

    private fun drawNode(canvas: Canvas, node: FlowNode) {
        val w = node.width
        val h = node.height
        val left = node.x
        val top = node.y
        val right = left + w
        val bottom = top + h
        val radius = node.corner.coerceIn(0f, min(w, h) / 2f)
        val alpha = (node.opacity.coerceIn(0.2f, 1f) * 255f).roundToInt()

        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, fill(SURFACE, alpha))
        canvas.drawRoundRect(
            RectF(left + 0.6f, top + 0.6f, right - 0.6f, bottom - 0.6f),
            radius,
            radius,
            stroke(OUTLINE, 1.2f, 180),
        )

        val platform = platformInfo(node.platform)
        val badgeColor = platform.color.toInt()
        canvas.roundBox(left + 10f, top + 9f, left + 34f, top + 25f, 4f, badgeColor)
        canvas.centeredEllipsis(platform.short, left + 22f, top + 17f, 20f, 9f, WHITE, true)

        val namePaint = textPaint(10.5f, ON_SURFACE, true)
        canvas.drawText(
            ellipsize(node.name, namePaint, (w - 52f).coerceAtLeast(20f)),
            left + 40f,
            top + 21.5f,
            namePaint,
        )

        canvas.save()
        canvas.clipRect(left + 6f, top + 30f, right - 6f, bottom - 6f)
        drawNodeBody(canvas, node, left, top + 32f, w, h - 38f)
        canvas.restore()
    }

    private fun drawNodeBody(canvas: Canvas, node: FlowNode, x: Float, y: Float, w: Float, h: Float) {
        val inset = 8f
        val left = x + inset
        val top = y + inset
        val right = x + w - inset
        val bottom = y + h - inset
        val innerW = (right - left).coerceAtLeast(4f)
        val innerH = (bottom - top).coerceAtLeast(4f)
        val accent = parseAccent(node.accent)
        val label = node.text.ifBlank { node.name }

        when (node.kind) {
            "screen" -> {
                canvas.roundBox(left, top, right, bottom, 12f, WHITE, OUTLINE_VARIANT, 1f)
                val barH = min(30f, innerH * 0.16f)
                canvas.roundBox(left, top, right, top + barH, 12f, accent)
                canvas.drawRect(left, top + barH - 12f, right, top + barH, fill(accent))
                canvas.text(ellipsize(label, textPaint(10f, WHITE, true), innerW - 16f), left + 10f, top + barH / 2f + 3.5f, 10f, WHITE, true)
                val contentTop = top + barH + 10f
                val blockH = ((bottom - contentTop - 18f) / 2f).coerceAtLeast(6f)
                canvas.roundBox(left + 10f, contentTop, right - 10f, contentTop + blockH, 6f, PRIMARY_CONTAINER)
                canvas.roundBox(left + 10f, contentTop + blockH + 8f, right - 10f, contentTop + blockH * 2f + 8f, 6f, SECONDARY_CONTAINER)
                if (innerH > 120f) {
                    canvas.roundBox(left + 10f, bottom - 24f, right - 10f, bottom, 8f, SURFACE_HIGH)
                }
            }
            "dialog" -> {
                canvas.roundBox(left + 6f, top + 4f, right - 6f, bottom - 4f, 14f, WHITE, OUTLINE_VARIANT)
                canvas.text(ellipsize(label, textPaint(11f, ON_SURFACE, true), innerW - 26f), left + 14f, top + 22f, 11f, ON_SURFACE, true)
                canvas.line(left + 14f, top + 32f, right - 14f, top + 32f, OUTLINE_VARIANT, 1f)
                canvas.line(left + 14f, top + 40f, right - 24f, top + 40f, OUTLINE_VARIANT, 2f)
                canvas.text("取消", right - 46f, bottom - 12f, 9f, PRIMARY, true)
                canvas.text("确定", right - 20f, bottom - 12f, 9f, PRIMARY, true)
            }
            "sheet" -> {
                canvas.roundBox(left, top + 14f, right, bottom, 14f, WHITE, OUTLINE_VARIANT)
                canvas.roundBox(x + w / 2f - 16f, top + 2f, x + w / 2f + 16f, top + 6f, 2f, OUTLINE)
                canvas.text(ellipsize(label, textPaint(11f, ON_SURFACE, true), innerW - 20f), left + 14f, top + 34f, 11f, ON_SURFACE, true)
                canvas.line(left + 14f, top + 46f, right - 14f, top + 46f, OUTLINE_VARIANT, 2f)
                canvas.line(left + 14f, top + 56f, right - 30f, top + 56f, OUTLINE_VARIANT, 2f)
            }
            "topbar" -> {
                canvas.roundBox(left, top, right, top + 42f, 10f, accent)
                canvas.text(ellipsize(label, textPaint(11f, WHITE, true), innerW - 36f), left + 12f, top + 26f, 11f, WHITE, true)
                for (i in 0..1) {
                    canvas.drawCircle(right - 14f - i * 14f, top + 21f, 3.2f, fill(WHITE, 220))
                }
            }
            "bottomappbar" -> {
                canvas.roundBox(left, bottom - 38f, right, bottom, 10f, SECONDARY_CONTAINER)
                canvas.text(ellipsize(label, textPaint(9f, ON_SURFACE_VARIANT), innerW - 20f), left + 10f, bottom - 14f, 9f, ON_SURFACE_VARIANT)
            }
            "bottomnav" -> {
                canvas.roundBox(left, bottom - 44f, right, bottom, 10f, SURFACE_HIGH)
                val step = innerW / 3f
                for (i in 0..2) {
                    val cx = left + step * (i + 0.5f)
                    canvas.drawCircle(cx, bottom - 28f, 4f, fill(if (i == 0) PRIMARY else OUTLINE))
                    canvas.line(cx - 10f, bottom - 13f, cx + 10f, bottom - 13f, if (i == 0) PRIMARY else OUTLINE_VARIANT, 2f)
                }
            }
            "navrail" -> {
                canvas.roundBox(left, top, left + 58f, bottom, 10f, SURFACE_HIGH)
                for (i in 0..2) {
                    val cy = top + 26f + i * 34f
                    canvas.drawCircle(left + 29f, cy, 10f, fill(if (i == 0) PRIMARY_CONTAINER else WHITE))
                    canvas.drawCircle(left + 29f, cy, 3f, fill(if (i == 0) PRIMARY else OUTLINE))
                }
            }
            "tabs" -> {
                canvas.line(left, bottom - 4f, right, bottom - 4f, OUTLINE_VARIANT, 2f)
                val step = innerW / 3f
                for (i in 0..2) {
                    val cx = left + step * (i + 0.5f)
                    canvas.centeredEllipsis(if (i == 0) label else "标签", cx, top + innerH / 2f, step - 6f, 9f, if (i == 0) PRIMARY else ON_SURFACE_VARIANT, i == 0)
                }
                canvas.line(left + 4f, bottom - 3f, left + step - 4f, bottom - 3f, PRIMARY, 3f)
            }
            "button", "buttonTonal", "buttonOutlined", "buttonText", "extFab", "fab", "iconButton" -> {
                val bg = when (node.kind) {
                    "buttonTonal" -> PRIMARY_CONTAINER
                    "buttonOutlined", "buttonText", "iconButton" -> WHITE
                    else -> accent
                }
                val fg = when (node.kind) {
                    "buttonTonal" -> ON_PRIMARY_CONTAINER
                    "buttonOutlined", "buttonText", "iconButton" -> PRIMARY
                    else -> WHITE
                }
                if (node.kind == "fab") {
                    canvas.drawCircle(x + w / 2f, y + h / 2f, min(innerW, innerH) * 0.42f, fill(bg))
                    canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW * 0.7f, 16f, fg, true)
                } else {
                    val boxH = if (node.kind == "extFab") min(innerH, 38f) else innerH
                    canvas.roundBox(left, top + (innerH - boxH) / 2f, right, top + (innerH + boxH) / 2f, boxH / 2f, bg, if (node.kind == "buttonOutlined") PRIMARY else null, 1.2f)
                    canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW - 12f, 11f, fg, true)
                }
            }
            "textfield", "textfieldOutlined", "search", "textarea" -> {
                val boxH = if (node.kind == "textarea") innerH else min(innerH, 38f)
                canvas.roundBox(left, top + 4f, right, top + 4f + boxH, 8f, WHITE, OUTLINE, 1.2f)
                canvas.text(ellipsize(label, textPaint(10f, ON_SURFACE_VARIANT), innerW - 24f), left + 12f, top + 4f + boxH / 2f + 3.5f, 10f, ON_SURFACE_VARIANT)
                if (node.kind == "search") {
                    canvas.drawCircle(right - 16f, top + 4f + boxH / 2f - 2f, 4f, stroke(ON_SURFACE_VARIANT, 1.4f))
                    canvas.line(right - 13f, top + 4f + boxH / 2f + 1f, right - 10f, top + 4f + boxH / 2f + 4f, ON_SURFACE_VARIANT, 1.4f)
                }
            }
            "switch" -> {
                val trackW = min(innerW, 44f)
                val trackH = min(innerH, 24f)
                val l = x + w / 2f - trackW / 2f
                val t = y + h / 2f - trackH / 2f
                canvas.roundBox(l, t, l + trackW, t + trackH, trackH / 2f, accent)
                canvas.drawCircle(l + trackW - trackH / 2f - 0.5f, t + trackH / 2f, trackH / 2f - 3f, fill(WHITE))
            }
            "checkbox" -> {
                val size = min(min(innerW, innerH), 24f)
                val l = x + w / 2f - size / 2f
                val t = y + h / 2f - size / 2f
                canvas.roundBox(l, t, l + size, t + size, 4f, accent)
                val path = Path().apply {
                    moveTo(l + size * 0.24f, t + size * 0.52f)
                    lineTo(l + size * 0.43f, t + size * 0.72f)
                    lineTo(l + size * 0.78f, t + size * 0.3f)
                }
                canvas.drawPath(path, stroke(WHITE, 2f))
            }
            "radio" -> {
                val r = min(min(innerW, innerH), 24f) / 2f
                canvas.drawCircle(x + w / 2f, y + h / 2f, r, stroke(accent, 2f))
                canvas.drawCircle(x + w / 2f, y + h / 2f, r * 0.5f, fill(accent))
            }
            "slider" -> {
                val cy = y + h / 2f
                canvas.line(left + 4f, cy, right - 4f, cy, OUTLINE_VARIANT, 3f)
                canvas.line(left + 4f, cy, left + innerW * 0.62f, cy, accent, 3f)
                canvas.drawCircle(left + innerW * 0.62f, cy, 8f, fill(accent))
            }
            "chip" -> {
                val boxH = min(innerH, 28f)
                canvas.roundBox(left, y + h / 2f - boxH / 2f, right, y + h / 2f + boxH / 2f, boxH / 2f, SECONDARY_CONTAINER, OUTLINE_VARIANT, 1f)
                canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW - 16f, 10f, ON_SURFACE, true)
            }
            "card" -> {
                canvas.roundBox(left, top, right, bottom, 10f, WHITE, OUTLINE_VARIANT, 1f)
                canvas.roundBox(left, top, right, top + innerH * 0.42f, 10f, PRIMARY_CONTAINER)
                canvas.text(ellipsize(label, textPaint(10.5f, ON_SURFACE, true), innerW - 18f), left + 10f, top + innerH * 0.42f + 16f, 10.5f, ON_SURFACE, true)
                canvas.line(left + 10f, top + innerH * 0.42f + 26f, right - 16f, top + innerH * 0.42f + 26f, OUTLINE_VARIANT, 2f)
            }
            "list" -> {
                canvas.roundBox(left, top, right, bottom, 8f, WHITE, OUTLINE_VARIANT, 1f)
                val rows = 3
                val rowH = innerH / rows
                for (i in 0 until rows) {
                    val cy = top + rowH * (i + 0.5f)
                    canvas.drawCircle(left + 16f, cy, 8f, fill(PRIMARY_CONTAINER))
                    canvas.drawCircle(left + 16f, cy, 3f, fill(PRIMARY))
                    canvas.line(left + 32f, cy - 4f, right - 12f, cy - 4f, OUTLINE_VARIANT, 2f)
                    canvas.line(left + 32f, cy + 4f, right - 30f, cy + 4f, OUTLINE_VARIANT, 1f)
                    if (i < rows - 1) canvas.line(left + 10f, top + rowH * (i + 1), right - 10f, top + rowH * (i + 1), OUTLINE_VARIANT, 0.7f)
                }
            }
            "image" -> {
                canvas.roundBox(left, top, right, bottom, 8f, PRIMARY_CONTAINER)
                val sunX = left + innerW * 0.72f
                val sunY = top + innerH * 0.28f
                canvas.drawCircle(sunX, sunY, min(innerW, innerH) * 0.09f, fill(PRIMARY, 120))
                val path = Path().apply {
                    moveTo(left + 6f, bottom - 6f)
                    lineTo(left + innerW * 0.3f, top + innerH * 0.45f)
                    lineTo(left + innerW * 0.52f, top + innerH * 0.7f)
                    lineTo(left + innerW * 0.68f, top + innerH * 0.52f)
                    lineTo(right - 6f, bottom - 6f)
                    close()
                }
                canvas.drawPath(path, fill(PRIMARY, 150))
            }
            "avatar" -> {
                val r = min(innerW, innerH) / 2f
                canvas.drawCircle(x + w / 2f, y + h / 2f, r, fill(PRIMARY_CONTAINER))
                canvas.drawCircle(x + w / 2f, y + h / 2f - r * 0.22f, r * 0.3f, fill(PRIMARY))
                canvas.roundBox(
                    x + w / 2f - r * 0.48f,
                    y + h / 2f + r * 0.12f,
                    x + w / 2f + r * 0.48f,
                    y + h / 2f + r * 0.58f,
                    r * 0.3f,
                    PRIMARY,
                )
            }
            "icon" -> {
                canvas.drawCircle(x + w / 2f, y + h / 2f, min(innerW, innerH) * 0.38f, fill(SECONDARY_CONTAINER))
                canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW * 0.7f, min(innerW, innerH) * 0.4f, PRIMARY, true)
            }
            "badge" -> {
                val boxH = min(innerH, 20f)
                canvas.roundBox(left + 4f, y + h / 2f - boxH / 2f, right - 4f, y + h / 2f + boxH / 2f, boxH / 2f, ERROR)
                canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW - 14f, 9f, WHITE, true)
            }
            "chart" -> {
                val baseline = bottom - 6f
                val barW = innerW / 7f
                val heights = listOf(0.35f, 0.6f, 0.48f, 0.82f, 0.7f)
                for ((i, v) in heights.withIndex()) {
                    val l = left + 6f + i * barW * 1.35f
                    canvas.roundBox(l, baseline - innerH * v, l + barW, baseline, 3f, if (i == 3) PRIMARY else PRIMARY_CONTAINER)
                }
                canvas.line(left, baseline, right, baseline, OUTLINE_VARIANT, 1f)
            }
            "map" -> {
                canvas.roundBox(left, top, right, bottom, 10f, 0xFFE8F0E4.toInt())
                for (i in 1..3) {
                    canvas.line(left + innerW * i / 4f, top, left + innerW * i / 4f, bottom, OUTLINE_VARIANT, 0.8f)
                    canvas.line(left, top + innerH * i / 4f, right, top + innerH * i / 4f, OUTLINE_VARIANT, 0.8f)
                }
                val px = left + innerW * 0.58f
                val py = top + innerH * 0.46f
                canvas.drawCircle(px, py, 5f, fill(ERROR))
                canvas.drawCircle(px, py, 2f, fill(WHITE))
            }
            "video", "web" -> {
                canvas.roundBox(left, top, right, bottom, 8f, if (node.kind == "video") DARK else WHITE, OUTLINE_VARIANT, 1f)
                if (node.kind == "video") {
                    val path = Path().apply {
                        moveTo(x + w / 2f - 6f, y + h / 2f - 10f)
                        lineTo(x + w / 2f + 12f, y + h / 2f)
                        lineTo(x + w / 2f - 6f, y + h / 2f + 10f)
                        close()
                    }
                    canvas.drawPath(path, fill(WHITE))
                } else {
                    canvas.roundBox(left, top, right, top + 16f, 8f, SURFACE_HIGH)
                    for (i in 0..2) {
                        canvas.drawCircle(left + 9f + i * 7f, top + 8f, 2f, fill(if (i == 0) ERROR else OUTLINE))
                    }
                    canvas.line(left + 10f, top + 30f, right - 10f, top + 30f, PRIMARY_CONTAINER, 6f)
                    canvas.line(left + 10f, top + 46f, right - 40f, top + 46f, OUTLINE_VARIANT, 4f)
                }
            }
            "snackbar" -> {
                canvas.roundBox(left, y + h / 2f - 16f, right, y + h / 2f + 16f, 8f, DARK)
                canvas.text(ellipsize(label, textPaint(10f, WHITE), innerW - 56f), left + 12f, y + h / 2f + 3.5f, 10f, WHITE)
                canvas.text("撤销", right - 12f, y + h / 2f + 3.5f, 9f, PRIMARY_CONTAINER, true)
            }
            "progress" -> {
                val cy = y + h / 2f
                canvas.roundBox(left, cy - 4f, right, cy + 4f, 4f, SURFACE_HIGH)
                canvas.roundBox(left, cy - 4f, left + innerW * 0.62f, cy + 4f, 4f, PRIMARY)
            }
            "calendar" -> {
                canvas.roundBox(left, top, right, bottom, 8f, WHITE, OUTLINE_VARIANT, 1f)
                canvas.text(ellipsize(label, textPaint(10f, PRIMARY, true), innerW - 16f), left + 10f, top + 16f, 10f, PRIMARY, true)
                val gridTop = top + 24f
                val cellW = innerW / 7f
                val cellH = (bottom - gridTop - 4f) / 4f
                for (r in 0..3) {
                    for (col in 0..6) {
                        val cx = left + cellW * (col + 0.5f)
                        val cy = gridTop + cellH * (r + 0.5f)
                        canvas.drawCircle(cx, cy, 2f, fill(if (r == 1 && col == 3) PRIMARY else OUTLINE_VARIANT))
                    }
                }
            }
            "menu" -> {
                canvas.roundBox(left, top, right, bottom, 8f, WHITE, OUTLINE_VARIANT, 1f)
                val rowH = innerH / 4f
                for (i in 0..3) {
                    val cy = top + rowH * (i + 0.5f)
                    canvas.drawCircle(left + 12f, cy, 2.4f, fill(if (i == 0) PRIMARY else OUTLINE))
                    canvas.line(left + 22f, cy, right - 12f, cy, if (i == 0) PRIMARY else OUTLINE_VARIANT, 2f)
                }
            }
            "link" -> {
                canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f - 2f, innerW - 8f, 11f, PRIMARY, true)
                canvas.line(left + 8f, y + h / 2f + 6f, right - 8f, y + h / 2f + 6f, PRIMARY, 1.2f)
            }
            "table" -> {
                canvas.roundBox(left, top, right, bottom, 6f, WHITE, OUTLINE_VARIANT, 1f)
                for (i in 0..3) {
                    val ly = top + innerH * i / 4f
                    canvas.line(left, ly, right, ly, OUTLINE_VARIANT, 1f)
                }
                for (i in 1..3) {
                    val lx = left + innerW * i / 4f
                    canvas.line(lx, top, lx, bottom, OUTLINE_VARIANT, 1f)
                }
                canvas.drawRect(left, top, right, top + innerH / 4f, fill(PRIMARY_CONTAINER))
            }
            "condition" -> {
                val path = Path().apply {
                    moveTo(x + w / 2f, top)
                    lineTo(right, y + h / 2f)
                    lineTo(x + w / 2f, bottom)
                    lineTo(left, y + h / 2f)
                    close()
                }
                canvas.drawPath(path, fill(TERTIARY_CONTAINER))
                canvas.drawPath(path, stroke(PRIMARY, 1.4f))
                canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW * 0.62f, 11f, ON_SURFACE, true)
            }
            "state" -> {
                canvas.roundBox(left, top, right, bottom, 12f, SECONDARY_CONTAINER, OUTLINE_VARIANT)
                canvas.drawCircle(left + 14f, y + h / 2f, 4f, fill(PRIMARY))
                canvas.text(ellipsize(label, textPaint(11f, ON_SURFACE, true), innerW - 30f), left + 24f, y + h / 2f + 3.5f, 11f, ON_SURFACE, true)
            }
            "api" -> {
                canvas.roundBox(left, top + 4f, right, bottom - 4f, 10f, 0xFFE0F2F1.toInt(), OUTLINE_VARIANT)
                canvas.text(ellipsize(label, textPaint(11f, ON_SURFACE, true), innerW - 36f), left + 12f, y + h / 2f + 3.5f, 11f, ON_SURFACE, true)
                canvas.text("⇄", right - 18f, y + h / 2f + 4f, 12f, PRIMARY, true)
            }
            "timer" -> {
                canvas.drawCircle(x + w / 2f, y + h / 2f, min(innerW, innerH) * 0.42f, fill(PRIMARY_CONTAINER))
                canvas.drawCircle(x + w / 2f, y + h / 2f, min(innerW, innerH) * 0.42f, stroke(PRIMARY, 1.2f))
                canvas.line(x + w / 2f, y + h / 2f, x + w / 2f, y + h / 2f - min(innerW, innerH) * 0.24f, PRIMARY, 1.6f)
                canvas.line(x + w / 2f, y + h / 2f, x + w / 2f + min(innerW, innerH) * 0.18f, y + h / 2f, PRIMARY, 1.6f)
                canvas.centeredEllipsis(label, x + w / 2f, bottom - 2f, innerW - 8f, 8f, ON_SURFACE_VARIANT)
            }
            "note" -> {
                canvas.roundBox(left, top, right, bottom, 6f, 0xFFFFF3CD.toInt(), 0xFFE0C36A.toInt(), 1f)
                canvas.text(ellipsize(label, textPaint(10f, 0xFF5B4A00.toInt(), true), innerW - 16f), left + 10f, top + 16f, 10f, 0xFF5B4A00.toInt(), true)
                canvas.line(left + 10f, top + 26f, right - 16f, top + 26f, 0xFFB9A24A.toInt(), 1.2f)
                canvas.line(left + 10f, top + 34f, right - 28f, top + 34f, 0xFFB9A24A.toInt(), 1.2f)
            }
            "group" -> {
                val paint = stroke(OUTLINE, 1.2f)
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
                canvas.drawRoundRect(RectF(left, top, right, bottom), 12f, 12f, paint)
                canvas.text(ellipsize(label, textPaint(9f, ON_SURFACE_VARIANT), innerW - 12f), left + 8f, top + 12f, 9f, ON_SURFACE_VARIANT)
            }
            "start", "end" -> {
                canvas.roundBox(left, y + h / 2f - 16f, right, y + h / 2f + 16f, 16f, if (node.kind == "start") PRIMARY else DARK)
                canvas.centeredEllipsis(label, x + w / 2f, y + h / 2f, innerW - 12f, 11f, WHITE, true)
            }
            else -> {
                val paint = stroke(OUTLINE, 1.2f)
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
                canvas.drawRoundRect(RectF(left, top, right, bottom), 10f, 10f, paint)
                canvas.centeredEllipsis("?", x + w / 2f, y + h / 2f - 4f, innerW - 8f, 22f, OUTLINE, true)
                canvas.centeredEllipsis(label, x + w / 2f, bottom - 8f, innerW - 8f, 8f, ON_SURFACE_VARIANT)
            }
        }
    }

    private fun parseAccent(value: String): Int {
        if (value.isBlank()) return PRIMARY
        return try {
            val cleaned = value.trim().removePrefix("#")
            when (cleaned.length) {
                6 -> (0xFF000000L or cleaned.toLong(16)).toInt()
                8 -> cleaned.toLong(16).toInt()
                else -> PRIMARY
            }
        } catch (_: Exception) {
            PRIMARY
        }
    }
}
