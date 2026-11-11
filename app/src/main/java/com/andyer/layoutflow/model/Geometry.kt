package com.andyer.layoutflow.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class EdgeGeometry(
    val startX: Float,
    val startY: Float,
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float,
    val endX: Float,
    val endY: Float,
) {
    fun pointAt(t: Float): Pair<Float, Float> {
        val mt = 1f - t
        val x = mt * mt * mt * startX + 3f * mt * mt * t * c1x + 3f * mt * t * t * c2x + t * t * t * endX
        val y = mt * mt * mt * startY + 3f * mt * mt * t * c1y + 3f * mt * t * t * c2y + t * t * t * endY
        return x to y
    }
}

fun edgeGeometry(project: FlowProject, edge: FlowEdge): EdgeGeometry? {
    val from = project.nodeById(edge.from) ?: return null
    val to = project.nodeById(edge.to) ?: return null
    return if (from.id == to.id) {
        val x = from.x + from.width
        val y = from.y + from.height / 2f
        EdgeGeometry(
            startX = x,
            startY = y,
            c1x = x + 90f,
            c1y = from.y - 40f,
            c2x = x + 90f,
            c2y = from.y + from.height + 40f,
            endX = x,
            endY = y,
        )
    } else {
        val startX = from.x + from.width
        val startY = from.y + from.height / 2f
        val endX = to.x
        val endY = to.y + to.height / 2f
        val distance = abs(endX - startX)
        val bend = max(60f, distance * 0.5f)
        EdgeGeometry(
            startX = startX,
            startY = startY,
            c1x = startX + bend,
            c1y = startY,
            c2x = endX - bend,
            c2y = endY,
            endX = endX,
            endY = endY,
        )
    }
}

fun arrowHead(
    fromX: Float,
    fromY: Float,
    toX: Float,
    toY: Float,
    length: Float,
    width: Float,
): List<Pair<Float, Float>> {
    val dx = toX - fromX
    val dy = toY - fromY
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val ux = dx / len
    val uy = dy / len
    val baseX = toX - ux * length
    val baseY = toY - uy * length
    val px = -uy * width / 2f
    val py = ux * width / 2f
    return listOf(
        toX to toY,
        baseX + px to baseY + py,
        baseX - px to baseY - py,
    )
}
