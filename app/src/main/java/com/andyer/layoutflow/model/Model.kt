package com.andyer.layoutflow.model

import kotlinx.serialization.Serializable

@Serializable
data class FlowProject(
    val schema: Int = 1,
    val name: String = "未命名流程",
    val nodes: List<FlowNode> = emptyList(),
    val edges: List<FlowEdge> = emptyList(),
    val snapToGrid: Boolean = true,
    val gridSize: Int = 8,
    val nextNodeSeq: Int = 1,
    val nextEdgeSeq: Int = 1,
    val updatedAt: Long = 0L,
)

@Serializable
data class FlowNode(
    val id: String,
    val specId: String,
    val name: String,
    val platform: String,
    val kind: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String = "",
    val accent: String = "",
    val corner: Float = 16f,
    val elevation: Float = 0f,
    val opacity: Float = 1f,
    val properties: Map<String, String> = emptyMap(),
)

@Serializable
data class FlowEdge(
    val id: String,
    val from: String,
    val to: String,
    val trigger: String = "click",
    val action: String = "navigate",
    val transition: String = "sharedAxisX",
    val duration: Int = 300,
    val delay: Int = 0,
    val easing: String = "emphasized",
    val condition: String = "",
    val label: String = "",
    val properties: Map<String, String> = emptyMap(),
)

fun FlowProject.nodeById(id: String?): FlowNode? = nodes.firstOrNull { it.id == id }

fun FlowProject.edgeById(id: String?): FlowEdge? = edges.firstOrNull { it.id == id }

fun FlowNode.contains(worldX: Float, worldY: Float): Boolean =
    worldX >= x && worldX <= x + width && worldY >= y && worldY <= y + height

fun FlowProject.bounds(): RectD? {
    if (nodes.isEmpty()) return null
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    for (node in nodes) {
        left = minOf(left, node.x)
        top = minOf(top, node.y)
        right = maxOf(right, node.x + node.width)
        bottom = maxOf(bottom, node.y + node.height)
    }
    return RectD(left, top, right, bottom)
}

data class RectD(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
