package com.andyer.layoutflow.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andyer.layoutflow.model.FlowNode
import com.andyer.layoutflow.model.platformInfo
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FlowNodeCard(
    node: FlowNode,
    selected: Boolean,
    connectionTarget: Boolean,
    scale: Float,
    pan: Offset,
    density: Float,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onDragPosition: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onResizeStart: () -> Unit,
    onResize: (Float, Float) -> Unit,
    onResizeEnd: () -> Unit,
    onPortStart: (Boolean, Offset) -> Unit,
    onPortDrag: (Offset) -> Unit,
    onPortEnd: (Offset) -> Unit,
    onPortCancel: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val platform = platformInfo(node.platform)
    val latestNode by rememberUpdatedState(node)
    val latestPan by rememberUpdatedState(pan)
    val latestScale by rememberUpdatedState(scale)
    val latestDensity by rememberUpdatedState(density)
    val shape = RoundedCornerShape(node.corner.dp)
    val borderColor = when {
        selected -> scheme.primary
        connectionTarget -> scheme.tertiary
        else -> scheme.outlineVariant
    }
    val borderWidth = if (selected || connectionTarget) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .offset { IntOffset((node.x * density).roundToInt(), (node.y * density).roundToInt()) }
            .size(node.width.dp, node.height.dp)
            .pointerInput(node.id) {
                detectTapGestures { onTap() }
            }
            .pointerInput(node.id) {
                var startTopLeft = Offset.Zero
                var accumulated = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        startTopLeft = Offset(
                            latestPan.x + latestNode.x * latestDensity * latestScale,
                            latestPan.y + latestNode.y * latestDensity * latestScale,
                        )
                        accumulated = Offset.Zero
                        onDragStart()
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        accumulated += delta
                        onMove(delta.x / latestDensity, delta.y / latestDensity)
                        onDragPosition(
                            startTopLeft + accumulated * latestScale +
                                Offset(
                                    latestNode.width * latestDensity * latestScale / 2f,
                                    latestNode.height * latestDensity * latestScale / 2f,
                                ),
                        )
                    },
                    onDragEnd = {
                        val center = startTopLeft + accumulated * latestScale +
                            Offset(
                                latestNode.width * latestDensity * latestScale / 2f,
                                latestNode.height * latestDensity * latestScale / 2f,
                            )
                        onDragEnd(center)
                    },
                    onDragCancel = {
                        onDragEnd(
                            startTopLeft + Offset(
                                latestNode.width * latestDensity * latestScale / 2f,
                                latestNode.height * latestDensity * latestScale / 2f,
                            ),
                        )
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .offset(y = (-17).dp)
                .width(node.width.dp)
                .height(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
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
                color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = node.opacity }
                .shadow(node.elevation.dp, shape)
                .clip(shape)
                .background(scheme.surfaceContainerLow)
                .border(borderWidth, borderColor, shape),
        ) {
            NodePreview(node = node, accent = accentColor(node.accent), modifier = Modifier.fillMaxSize())
        }

        PortDot(
            isOut = false,
            selected = selected,
            density = density,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-9).dp)
                .pointerInput(node.id) {
                    var screen = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            screen = Offset(
                                latestPan.x + latestNode.x * latestDensity * latestScale,
                                latestPan.y + (latestNode.y + latestNode.height / 2f) * latestDensity * latestScale,
                            )
                            onPortStart(false, screen)
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            screen += delta * latestScale
                            onPortDrag(screen)
                        },
                        onDragEnd = { onPortEnd(screen) },
                        onDragCancel = { onPortCancel() },
                    )
                },
        )
        PortDot(
            isOut = true,
            selected = selected,
            density = density,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 9.dp)
                .pointerInput(node.id) {
                    var screen = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            screen = Offset(
                                latestPan.x + (latestNode.x + latestNode.width) * latestDensity * latestScale,
                                latestPan.y + (latestNode.y + latestNode.height / 2f) * latestDensity * latestScale,
                            )
                            onPortStart(true, screen)
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            screen += delta * latestScale
                            onPortDrag(screen)
                        },
                        onDragEnd = { onPortEnd(screen) },
                        onDragCancel = { onPortCancel() },
                    )
                },
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 7.dp, y = 7.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .border(2.dp, scheme.surface, CircleShape)
                    .pointerInput(node.id) {
                        detectDragGestures(
                            onDragStart = { onResizeStart() },
                            onDrag = { change, delta ->
                                change.consume()
                                onResize(delta.x / latestDensity, delta.y / latestDensity)
                            },
                            onDragEnd = { onResizeEnd() },
                        )
                    },
            )
        }
    }
}

@Composable
private fun PortDot(
    isOut: Boolean,
    selected: Boolean,
    density: Float,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 10.dp else 7.dp)
                .clip(CircleShape)
                .background(if (isOut) scheme.primary else scheme.surfaceContainerHighest)
                .border(1.5.dp, if (isOut) scheme.primary else scheme.outline, CircleShape),
        )
    }
}

fun accentColor(value: String): Color {
    if (value.isBlank()) return Color(0xFF6750A4)
    return runCatching {
        val clean = if (value.startsWith("#")) value else "#$value"
        Color(AndroidColor.parseColor(clean))
    }.getOrDefault(Color(0xFF6750A4))
}

@Composable
private fun NodePreview(node: FlowNode, accent: Color, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val label = node.text.ifBlank { node.name }
    val smallText = 9.sp
    Box(modifier = modifier.padding(5.dp)) {
        when (node.kind) {
            "screen" -> ScreenPreview(node, accent, label)
            "dialog" -> DialogPreview(label, accent)
            "sheet" -> SheetPreview(label, accent)
            "topbar" -> TopBarPreview(label, accent)
            "bottomappbar" -> BottomBarPreview(label)
            "bottomnav" -> BottomNavPreview(accent, label)
            "navrail" -> NavRailPreview(accent)
            "tabs" -> TabsPreview(label, accent)
            "button", "buttonTonal", "buttonOutlined", "buttonText" -> {
                val bg = when (node.kind) {
                    "buttonTonal" -> scheme.primaryContainer
                    "buttonOutlined", "buttonText" -> Color.Transparent
                    else -> accent
                }
                val fg = when (node.kind) {
                    "buttonTonal" -> scheme.onPrimaryContainer
                    "buttonOutlined", "buttonText" -> accent
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(bg)
                        .then(
                            if (node.kind == "buttonOutlined") {
                                Modifier.border(1.dp, accent, RoundedCornerShape(24.dp))
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = fg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            "fab" -> Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(node.width.dp.coerceAtMost(node.height.dp) * 0.68f)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            "extFab" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            "iconButton" -> Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(min(node.width, node.height).dp * 0.7f)
                    .clip(CircleShape)
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = accent, fontSize = 14.sp)
            }
            "textfield", "textfieldOutlined" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (node.kind == "textfield") scheme.surfaceVariant.copy(alpha = 0.45f) else scheme.surface)
                    .border(1.dp, if (node.kind == "textfieldOutlined") scheme.outline else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(label, color = scheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            "search" -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(10.dp)) {
                    drawCircle(accent, radius = size.minDimension * 0.32f, style = Stroke(width = 1.4f))
                    drawLine(accent, start = Offset(size.width * 0.68f, size.height * 0.68f), end = Offset(size.width * 0.95f, size.height * 0.95f), strokeWidth = 1.4f)
                }
                Spacer(Modifier.width(6.dp))
                Text(label, color = scheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1)
            }
            "switch" -> Canvas(Modifier.fillMaxSize()) {
                val trackH = size.height * 0.62f
                val trackW = (trackH * 1.75f).coerceAtMost(size.width * 0.86f)
                val left = (size.width - trackW) / 2f
                val top = (size.height - trackH) / 2f
                drawRoundRect(accent, Offset(left, top), Size(trackW, trackH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f))
                drawCircle(Color.White, radius = trackH / 2f - 2.5f, center = Offset(left + trackW - trackH / 2f, top + trackH / 2f))
            }
            "checkbox" -> Canvas(Modifier.fillMaxSize()) {
                val s = min(size.width, size.height) * 0.56f
                val left = (size.width - s) / 2f
                val top = (size.height - s) / 2f
                drawRoundRect(accent, Offset(left, top), Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.2f))
                val path = Path().apply {
                    moveTo(left + s * 0.22f, top + s * 0.52f)
                    lineTo(left + s * 0.43f, top + s * 0.72f)
                    lineTo(left + s * 0.8f, top + s * 0.3f)
                }
                drawPath(path, Color.White, style = Stroke(width = 1.8f))
            }
            "radio" -> Canvas(Modifier.fillMaxSize()) {
                val r = min(size.width, size.height) * 0.3f
                drawCircle(accent, radius = r, center = center, style = Stroke(width = 2f))
                drawCircle(accent, radius = r * 0.48f, center = center)
            }
            "slider" -> Canvas(Modifier.fillMaxSize()) {
                val cy = size.height / 2f
                drawLine(scheme.outlineVariant, Offset(4f, cy), Offset(size.width - 4f, cy), strokeWidth = 4f)
                drawLine(accent, Offset(4f, cy), Offset(size.width * 0.62f, cy), strokeWidth = 4f)
                drawCircle(accent, radius = size.height * 0.26f, center = Offset(size.width * 0.62f, cy))
            }
            "chip" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.secondaryContainer)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 9.sp, color = scheme.onSecondaryContainer, maxLines = 1)
            }
            "card" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.52f)
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontSize = 10.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Column(Modifier.padding(6.dp)) {
                    Line(scheme.outlineVariant, 0.55f)
                    Spacer(Modifier.height(4.dp))
                    Line(scheme.outlineVariant, 0.35f)
                }
            }
            "list" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface),
            ) {
                repeat(3) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)))
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Line(scheme.outlineVariant, 0.6f)
                            Spacer(Modifier.height(3.dp))
                            Line(scheme.outlineVariant, 0.35f)
                        }
                    }
                    if (index < 2) Line(scheme.outlineVariant.copy(alpha = 0.5f), 0.2f)
                }
            }
            "image" -> Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.22f)),
            ) {
                drawCircle(accent.copy(alpha = 0.5f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.72f, size.height * 0.28f))
                val path = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width * 0.3f, size.height * 0.45f)
                    lineTo(size.width * 0.52f, size.height * 0.7f)
                    lineTo(size.width * 0.7f, size.height * 0.5f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path, accent.copy(alpha = 0.55f))
            }
            "avatar" -> Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(min(node.width, node.height).dp * 0.72f)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(accent, radius = size.minDimension * 0.17f, center = Offset(size.width / 2f, size.height * 0.36f))
                    drawCircle(accent, radius = size.minDimension * 0.3f, center = Offset(size.width / 2f, size.height * 0.92f))
                }
            }
            "icon" -> Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(min(node.width, node.height).dp * 0.62f)
                    .clip(CircleShape)
                    .background(scheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(label.take(1), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            "badge" -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(min(node.height * 0.55f, 22f).dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(11.dp))
                    .background(scheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = scheme.onError, fontSize = 8.sp, maxLines = 1)
            }
            "progress" -> Canvas(Modifier.fillMaxSize()) {
                val cy = size.height / 2f
                drawRoundRect(scheme.surfaceVariant, Offset(0f, cy - 4f), Size(size.width, 8f), androidx.compose.ui.geometry.CornerRadius(4f))
                drawRoundRect(accent, Offset(0f, cy - 4f), Size(size.width * 0.62f, 8f), androidx.compose.ui.geometry.CornerRadius(4f))
            }
            "chart" -> Row(
                modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                listOf(0.35f, 0.6f, 0.45f, 0.82f, 0.68f).forEachIndexed { index, value ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(value)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (index == 3) accent else accent.copy(alpha = 0.35f)),
                    )
                }
            }
            "map" -> Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE8F0E4)),
            ) {
                for (i in 1..3) {
                    drawLine(scheme.outlineVariant, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), strokeWidth = 1f)
                    drawLine(scheme.outlineVariant, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), strokeWidth = 1f)
                }
                drawCircle(scheme.error, radius = size.minDimension * 0.08f, center = Offset(size.width * 0.58f, size.height * 0.44f))
                drawCircle(Color.White, radius = size.minDimension * 0.03f, center = Offset(size.width * 0.58f, size.height * 0.44f))
            }
            "video" -> Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF322F35)),
            ) {
                val path = Path().apply {
                    moveTo(size.width * 0.44f, size.height * 0.36f)
                    lineTo(size.width * 0.66f, size.height * 0.5f)
                    lineTo(size.width * 0.44f, size.height * 0.64f)
                    close()
                }
                drawPath(path, Color.White)
            }
            "web" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(14.dp).background(scheme.surfaceVariant).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(Modifier.size(3.dp).clip(CircleShape).background(scheme.error))
                    Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFE0A000)))
                    Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF3C9B4A)))
                }
                Column(Modifier.padding(6.dp)) {
                    Text(label, fontSize = 9.sp, color = scheme.onSurface, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Line(accent.copy(alpha = 0.55f), 0.5f)
                    Spacer(Modifier.height(3.dp))
                    Line(scheme.outlineVariant, 0.35f)
                }
            }
            "snackbar" -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(min(node.height * 0.5f, 30f).dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.inverseSurface)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(label, color = scheme.inverseOnSurface, fontSize = 8.sp, maxLines = 1)
            }
            "calendar" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(6.dp),
            ) {
                Text(label, fontSize = 8.sp, color = accent, fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                repeat(4) { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        repeat(7) { col ->
                            Box(
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(if (row == 1 && col == 3) accent else scheme.outlineVariant),
                            )
                        }
                    }
                }
            }
            "menu" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(4.dp),
            ) {
                repeat(4) { index ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(if (index == 0) accent else scheme.outline),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (index == 0) label else "菜单项",
                            fontSize = 8.sp,
                            color = if (index == 0) accent else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            "link" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.7f).height(1.dp).background(accent))
            }
            "table" -> Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(6.dp)),
            ) {
                for (i in 1..3) {
                    drawLine(scheme.outlineVariant, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), strokeWidth = 1f)
                }
                for (i in 1..3) {
                    drawLine(scheme.outlineVariant, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), strokeWidth = 1f)
                }
                drawRect(accent.copy(alpha = 0.2f), size = Size(size.width, size.height / 4f))
            }
            "textarea" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outline, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            ) {
                Text(label, fontSize = 8.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Line(scheme.outlineVariant, 0.45f)
                Spacer(Modifier.height(4.dp))
                Line(scheme.outlineVariant, 0.3f)
            }
            "condition" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 2f)
                        lineTo(size.width - 2f, size.height / 2f)
                        lineTo(size.width / 2f, size.height - 2f)
                        lineTo(2f, size.height / 2f)
                        close()
                    }
                    drawPath(path, scheme.tertiaryContainer)
                    drawPath(path, scheme.primary, style = Stroke(width = 1.4f))
                }
                Text(label, fontSize = 9.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            "state" -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.secondaryContainer)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize = 9.sp, color = scheme.onSecondaryContainer, maxLines = 1)
            }
            "api" -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE0F2F1))
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, modifier = Modifier.weight(1f), fontSize = 9.sp, color = scheme.onSurface, maxLines = 1)
                Text("⇄", fontSize = 12.sp, color = accent)
            }
            "timer" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * 0.36f
                    drawCircle(accent.copy(alpha = 0.2f), radius = r, center = center)
                    drawCircle(accent, radius = r, center = center, style = Stroke(width = 1.4f))
                    drawLine(accent, center, Offset(center.x, center.y - r * 0.58f), strokeWidth = 1.6f)
                    drawLine(accent, center, Offset(center.x + r * 0.42f, center.y), strokeWidth = 1.6f)
                }
                Text(label, fontSize = 8.sp, color = scheme.onSurface, modifier = Modifier.align(Alignment.BottomCenter))
            }
            "note" -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFF3CD))
                    .border(1.dp, Color(0xFFE0C36A), RoundedCornerShape(6.dp))
                    .padding(7.dp),
            ) {
                Text(label, fontSize = 8.sp, color = Color(0xFF5B4A00), fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Line(Color(0xFFB9A24A), 0.5f)
                Spacer(Modifier.height(4.dp))
                Line(Color(0xFFB9A24A), 0.35f)
            }
            "group" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, scheme.outline, RoundedCornerShape(12.dp),),
            ) {
                Text(label, fontSize = 8.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
            }
            "start", "end" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (node.kind == "start") accent else scheme.inverseSurface),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, scheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", fontSize = 18.sp, color = scheme.outline)
            }
        }
    }
}

@Composable
private fun Line(color: Color, widthFraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction.coerceIn(0.05f, 1f))
            .height(2.dp)
            .background(color, RoundedCornerShape(1.dp)),
    )
}

@Composable
private fun ScreenPreview(node: FlowNode, accent: Color, label: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(14.dp).padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("9:41", fontSize = 6.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(14.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(scheme.onSurfaceVariant))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(label, fontSize = 11.sp, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("内容区", fontSize = 9.sp, color = accent)
            }
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(scheme.surfaceContainerHigh))
            Box(Modifier.fillMaxWidth().weight(0.8f).clip(RoundedCornerShape(10.dp)).background(scheme.surfaceContainerHigh))
        }
    }
}

@Composable
private fun DialogPreview(label: String, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Line(scheme.outlineVariant, 0.8f)
        Spacer(Modifier.height(4.dp))
        Line(scheme.outlineVariant, 0.55f)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("取消", fontSize = 9.sp, color = accent, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(12.dp))
            Text("确定", fontSize = 9.sp, color = accent, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SheetPreview(label: String, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(10.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(24.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(scheme.outline))
        Spacer(Modifier.height(9.dp))
        Text(label, fontSize = 10.sp, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Line(scheme.outlineVariant, 0.85f)
        Spacer(Modifier.height(4.dp))
        Line(scheme.outlineVariant, 0.55f)
    }
}

@Composable
private fun TopBarPreview(label: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("≡", fontSize = 12.sp, color = accent)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text("⋮", fontSize = 12.sp, color = accent)
    }
}

@Composable
private fun BottomBarPreview(label: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, fontSize = 9.sp, color = scheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun BottomNavPreview(accent: Color, label: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(3) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (index == 0) accent.copy(alpha = 0.25f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) accent else scheme.outline),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    if (index == 0 && label.isNotBlank()) label.take(2) else "项目",
                    fontSize = 6.sp,
                    color = if (index == 0) accent else scheme.outline,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NavRailPreview(accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (index == 0) accent.copy(alpha = 0.22f) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (index == 0) accent else scheme.outline))
            }
        }
    }
}

@Composable
private fun TabsPreview(label: String, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (index == 0) label else "标签",
                    fontSize = 8.sp,
                    color = if (index == 0) accent else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth(if (index == 0) 0.7f else 0f)
                        .height(2.dp)
                        .background(accent),
                )
            }
        }
    }
}
