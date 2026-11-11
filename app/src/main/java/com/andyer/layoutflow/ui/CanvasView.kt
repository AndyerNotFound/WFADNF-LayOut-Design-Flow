package com.andyer.layoutflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andyer.layoutflow.model.FlowEdge
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.arrowHead
import com.andyer.layoutflow.model.contains
import com.andyer.layoutflow.model.displayLabel
import com.andyer.layoutflow.model.edgeGeometry
import com.andyer.layoutflow.model.platformInfo
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ConnectionDrag(
    val fromId: String,
    val fromIsOut: Boolean,
    val start: Offset,
    val current: Offset,
    val targetId: String? = null,
)

@Composable
fun FlowCanvas(
    vm: EditorViewModel,
    state: EditorUiState,
    onNodeDragStart: (String) -> Unit,
    onNodeDragPoint: (Offset) -> Unit,
    onNodeDragEnd: (String, Offset) -> Unit,
    onViewportSize: (IntSize) -> Unit,
    onAddFirst: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current.density
    val latestState by rememberUpdatedState(state)
    var connection by remember { mutableStateOf<ConnectionDrag?>(null) }
    val graphBox = Modifier.fillMaxSize()

    Box(
        modifier = graphBox
            .clipToBounds()
            .background(scheme.background)
            .onSizeChanged { onViewportSize(it) }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    vm.transformViewport(centroid, pan, zoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val snapshot = latestState
                    val worldX = (position.x - snapshot.pan.x) / snapshot.scale / density
                    val worldY = (position.y - snapshot.pan.y) / snapshot.scale / density
                    val node = snapshot.project.nodes.lastOrNull { it.contains(worldX, worldY) }
                    if (node != null) {
                        vm.selectNode(node.id)
                    } else {
                        val edge = hitTestEdge(snapshot.project, snapshot.pan, snapshot.scale, density, position)
                        if (edge != null) vm.selectEdge(edge.id) else vm.clearSelection()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGrid(scheme.outlineVariant, state.pan, state.scale, density)
            for (edge in state.project.edges) {
                val screen = screenEdge(state.project, edge, state.pan, state.scale, density) ?: continue
                drawScreenEdge(edge, screen, edge.id == state.selectedEdgeId, scheme.primary, scheme.outline)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.pan.x
                    translationY = state.pan.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            for (node in state.project.nodes) {
                key(node.id) {
                FlowNodeCard(
                    node = node,
                    selected = node.id == state.selectedNodeId,
                    connectionTarget = connection?.targetId == node.id,
                    scale = state.scale,
                    pan = state.pan,
                    density = density,
                    onTap = { vm.selectNode(node.id) },
                    onDragStart = {
                        vm.beginNodeDrag(node.id)
                        onNodeDragStart(node.id)
                    },
                    onMove = { dx, dy -> vm.moveNodeBy(node.id, dx, dy) },
                    onDragPosition = onNodeDragPoint,
                    onDragEnd = { point ->
                        vm.endNodeDrag(node.id)
                        onNodeDragEnd(node.id, point)
                    },
                    onResizeStart = { vm.beginNodeResize(node.id) },
                    onResize = { dx, dy -> vm.resizeNodeBy(node.id, dx, dy) },
                    onResizeEnd = { vm.endNodeDrag(node.id) },
                    onPortStart = { isOut, point ->
                        connection = ConnectionDrag(node.id, isOut, point, point)
                    },
                    onPortDrag = { point ->
                        val current = connection
                        if (current != null) {
                            val target = hitTestNode(
                                latestState.project,
                                latestState.pan,
                                latestState.scale,
                                density,
                                point,
                            )
                            connection = current.copy(
                                current = point,
                                targetId = target?.id?.takeIf { it != node.id },
                            )
                        }
                    },
                    onPortEnd = { point ->
                        val current = connection
                        if (current != null) {
                            val target = hitTestNode(
                                latestState.project,
                                latestState.pan,
                                latestState.scale,
                                density,
                                point,
                            )
                            if (target != null && target.id != node.id) {
                                if (current.fromIsOut) {
                                    vm.addEdge(current.fromId, target.id)
                                } else {
                                    vm.addEdge(target.id, current.fromId)
                                }
                            }
                        }
                        connection = null
                    },
                    onPortCancel = { connection = null },
                )
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            for (node in state.project.nodes) {
                val screenX = state.pan.x + node.x * density * state.scale
                val screenY = state.pan.y + node.y * density * state.scale - 15f * density
                val platform = platformInfo(node.platform)
                Row(
                    modifier = Modifier
                        .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                        .width((node.width * state.scale).coerceAtLeast(44f).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(platform.color.toInt())),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = node.name,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (node.id == state.selectedNodeId) scheme.primary else scheme.onSurfaceVariant,
                        fontWeight = if (node.id == state.selectedNodeId) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        if (state.project.nodes.isEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("画布是空的", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "从下方元件库长按拖入，或先添加一个页面",
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    TextButton(onClick = onAddFirst) { Text("添加页面") }
                }
            }
        }

        val activeConnection = connection
        if (activeConnection != null) {
            Canvas(Modifier.fillMaxSize()) {
                val start = activeConnection.start
                val end = activeConnection.current
                val bend = maxOf(50.dp.toPx(), abs(end.x - start.x) * 0.5f)
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(start.x + bend, start.y, end.x - bend, end.y, end.x, end.y)
                }
                drawPath(
                    path,
                    scheme.primary,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 9f)),
                    ),
                )
                drawCircle(scheme.primary, radius = 5.dp.toPx(), center = start)
                drawCircle(scheme.primary, radius = 6.dp.toPx(), center = end)
                if (activeConnection.targetId != null) {
                    drawCircle(scheme.tertiary.copy(alpha = 0.22f), radius = 22.dp.toPx(), center = end)
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            for (edge in state.project.edges) {
                if (state.scale < 0.45f && edge.id != state.selectedEdgeId) continue
                val screen = screenEdge(state.project, edge, state.pan, state.scale, density) ?: continue
                val (midX, midY) = screen.pointAt(0.5f)
                EdgeLabel(
                    edge = edge,
                    selected = edge.id == state.selectedEdgeId,
                    midX = midX,
                    midY = midY,
                    onClick = { vm.selectEdge(edge.id) },
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            shape = RoundedCornerShape(14.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${state.project.nodes.size} 元件 · ${state.project.edges.size} 连线 · ${(state.scale * 100).roundToInt()}%",
                        fontSize = 9.sp,
                        color = scheme.onSurfaceVariant,
                    )
                    if (state.project.snapToGrid) {
                        Text(" · 吸附", fontSize = 9.sp, color = scheme.primary)
                    }
                }
                Text(
                    if (state.scale < 0.45f) "放大后显示连线标注" else "长按元件拖入 · 拖动圆点连线",
                    fontSize = 8.sp,
                    color = scheme.outline,
                )
            }
        }
    }
}

@Composable
private fun EdgeLabel(
    edge: FlowEdge,
    selected: Boolean,
    midX: Float,
    midY: Float,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .offsetPx(midX, midY)
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height / 2f
            }
            .widthIn(max = 210.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.95f))
            .border(1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = edge.displayLabel(),
            fontSize = 8.5.sp,
            lineHeight = 10.sp,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.offsetPx(x: Float, y: Float): Modifier =
    this.then(
        Modifier.offset {
            IntOffset(x.roundToInt(), y.roundToInt())
        },
    )

private data class ScreenEdgeGeometry(
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

private fun screenEdge(
    project: FlowProject,
    edge: FlowEdge,
    pan: Offset,
    scale: Float,
    density: Float,
): ScreenEdgeGeometry? {
    val g = edgeGeometry(project, edge) ?: return null
    fun sx(value: Float) = pan.x + value * density * scale
    fun sy(value: Float) = pan.y + value * density * scale
    return ScreenEdgeGeometry(
        startX = sx(g.startX),
        startY = sy(g.startY),
        c1x = sx(g.c1x),
        c1y = sy(g.c1y),
        c2x = sx(g.c2x),
        c2y = sy(g.c2y),
        endX = sx(g.endX),
        endY = sy(g.endY),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScreenEdge(
    edge: FlowEdge,
    g: ScreenEdgeGeometry,
    selected: Boolean,
    primary: Color,
    outline: Color,
) {
    val color = if (selected) primary else outline
    val width = if (selected) 3.2.dp.toPx() else 2.dp.toPx()
    val path = Path().apply {
        moveTo(g.startX, g.startY)
        cubicTo(g.c1x, g.c1y, g.c2x, g.c2y, g.endX, g.endY)
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
    val head = arrowHead(g.c2x, g.c2y, g.endX, g.endY, 11.dp.toPx(), 9.dp.toPx())
    val headPath = Path().apply {
        moveTo(head[0].first, head[0].second)
        lineTo(head[1].first, head[1].second)
        lineTo(head[2].first, head[2].second)
        close()
    }
    drawPath(headPath, color)
    drawCircle(color, radius = if (selected) 4.5.dp.toPx() else 3.2.dp.toPx(), center = Offset(g.startX, g.startY))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    color: Color,
    pan: Offset,
    scale: Float,
    density: Float,
) {
    val base = 24.dp.toPx()
    var spacing = base * scale
    while (spacing < base) spacing *= 2f
    val dotColor = color.copy(alpha = 0.35f)
    val startX = ((pan.x % spacing) + spacing) % spacing
    val startY = ((pan.y % spacing) + spacing) % spacing
    var x = startX
    var ix = 0
    while (x < size.width) {
        var y = startY
        var iy = 0
        while (y < size.height) {
            val major = ix % 4 == 0 && iy % 4 == 0
            drawCircle(
                color = if (major) dotColor.copy(alpha = 0.55f) else dotColor,
                radius = if (major) 1.4.dp.toPx() else 0.9.dp.toPx(),
                center = Offset(x, y),
            )
            y += spacing
            iy++
        }
        x += spacing
        ix++
    }
}

private fun hitTestNode(
    project: FlowProject,
    pan: Offset,
    scale: Float,
    density: Float,
    point: Offset,
) = project.nodes.lastOrNull {
    val worldX = (point.x - pan.x) / scale / density
    val worldY = (point.y - pan.y) / scale / density
    it.contains(worldX, worldY)
}

private fun hitTestEdge(
    project: FlowProject,
    pan: Offset,
    scale: Float,
    density: Float,
    point: Offset,
): FlowEdge? {
    val threshold = 13.dp.value * density
    var best: FlowEdge? = null
    var bestDistance = threshold * threshold
    for (edge in project.edges) {
        val g = screenEdge(project, edge, pan, scale, density) ?: continue
        for (step in 0..28) {
            val (x, y) = g.pointAt(step / 28f)
            val dx = x - point.x
            val dy = y - point.y
            val d = dx * dx + dy * dy
            if (d < bestDistance) {
                bestDistance = d
                best = edge
            }
        }
    }
    return best
}
