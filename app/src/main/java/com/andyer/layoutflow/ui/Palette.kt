package com.andyer.layoutflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andyer.layoutflow.model.COMPONENTS
import com.andyer.layoutflow.model.ComponentSpec
import com.andyer.layoutflow.model.PLATFORMS
import com.andyer.layoutflow.model.QUICK_SPECS
import com.andyer.layoutflow.model.platformInfo
import kotlin.math.roundToInt

@Composable
fun PaletteShelf(
    onOpenCatalog: () -> Unit,
    onDragStart: (ComponentSpec, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset?) -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("元件库", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "长按拖入画布",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenCatalog) { Text("全部元件", fontSize = 11.sp) }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lazyItems(QUICK_SPECS, key = { it.id }) { spec ->
                    QuickSpecTile(
                        spec = spec,
                        onDragStart = { onDragStart(spec, it) },
                        onDragMove = onDragMove,
                        onDragEnd = onDragEnd,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSpecTile(
    spec: ComponentSpec,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val platform = platformInfo(spec.platform)
    var origin by remember { mutableStateOf(Offset.Zero) }
    var point by remember { mutableStateOf(Offset.Zero) }
    Surface(
        modifier = Modifier
            .width(104.dp)
            .height(62.dp)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(spec.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { local ->
                        point = origin + local
                        onDragStart(point)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        point += delta
                        onDragMove(point)
                    },
                    onDragEnd = { onDragEnd(point) },
                    onDragCancel = { onDragEnd(null) },
                )
            },
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComponentGlyph(spec = spec, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(7.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .background(Color(platform.color.toInt()), CircleShape),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(platform.label, fontSize = 7.sp, color = scheme.onSurfaceVariant)
                }
                Text(
                    spec.name,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSheet(
    onDismiss: () -> Unit,
    onAdd: (ComponentSpec) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dragGuard = rememberSheetDragBlocker()
    var query by remember { mutableStateOf("") }
    var platformFilter by remember { mutableStateOf("all") }
    val filtered = COMPONENTS.filter { spec ->
        (platformFilter == "all" || spec.platform == platformFilter) &&
            (query.isBlank() ||
                spec.name.contains(query, ignoreCase = true) ||
                spec.category.contains(query, ignoreCase = true) ||
                spec.description.contains(query, ignoreCase = true))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .nestedScroll(dragGuard)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("全部元件", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${COMPONENTS.size} 个跨平台组件与逻辑节点",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索组件、类别或用途") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = platformFilter == "all",
                        onClick = { platformFilter = "all" },
                        label = { Text("全部", fontSize = 10.sp) },
                    )
                }
                lazyItems(PLATFORMS, key = { it.id }) { platform ->
                    FilterChip(
                        selected = platformFilter == platform.id,
                        onClick = { platformFilter = platform.id },
                        label = { Text(platform.label, fontSize = 10.sp) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { spec ->
                    CatalogCard(spec = spec, onClick = { onAdd(spec) })
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(spec: ComponentSpec, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val platform = platformInfo(spec.platform)
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(Color(platform.color.toInt()), CircleShape),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${platform.label} · ${spec.category}",
                            fontSize = 8.sp,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        spec.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ComponentGlyph(spec = spec, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                spec.description,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${spec.width.roundToInt()} × ${spec.height.roundToInt()} dp",
                fontSize = 8.sp,
                color = scheme.outline,
            )
        }
    }
}

@Composable
private fun ComponentGlyph(spec: ComponentSpec, modifier: Modifier) {
    val color = Color(platformInfo(spec.platform).color.toInt())
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val soft = color.copy(alpha = 0.18f)
        when (spec.kind) {
            "screen", "web", "dialog", "sheet", "card", "group" -> {
                drawRoundRect(soft, Offset(w * 0.12f, h * 0.06f), Size(w * 0.76f, h * 0.88f), CornerRadius(w * 0.12f))
                drawRoundRect(color.copy(alpha = 0.55f), Offset(w * 0.12f, h * 0.06f), Size(w * 0.76f, h * 0.2f), CornerRadius(w * 0.1f))
                drawLine(color.copy(alpha = 0.4f), Offset(w * 0.26f, h * 0.5f), Offset(w * 0.74f, h * 0.5f), strokeWidth = w * 0.06f)
                drawLine(color.copy(alpha = 0.3f), Offset(w * 0.26f, h * 0.65f), Offset(w * 0.62f, h * 0.65f), strokeWidth = w * 0.05f)
            }
            "button", "buttonTonal", "buttonOutlined", "extFab", "chip", "badge", "start", "end" -> {
                drawRoundRect(color.copy(alpha = if (spec.kind == "buttonOutlined") 0.2f else 0.8f), Offset(0f, h * 0.3f), Size(w, h * 0.4f), CornerRadius(h * 0.2f))
                if (spec.kind == "buttonOutlined") {
                    drawRoundRect(color, Offset(0f, h * 0.3f), Size(w, h * 0.4f), CornerRadius(h * 0.2f), style = Stroke(width = 1.2f))
                }
            }
            "fab", "avatar", "radio", "timer" -> {
                drawCircle(soft, radius = w * 0.42f, center = center)
                drawCircle(color, radius = w * 0.26f, center = center)
            }
            "textfield", "textfieldOutlined", "search", "textarea", "link", "table" -> {
                drawRoundRect(soft, Offset(0f, h * 0.22f), Size(w, h * 0.56f), CornerRadius(w * 0.14f))
                drawRoundRect(color.copy(alpha = 0.7f), Offset(0f, h * 0.22f), Size(w, h * 0.56f), CornerRadius(w * 0.14f), style = Stroke(width = 1.2f))
                drawLine(color.copy(alpha = 0.4f), Offset(w * 0.16f, h * 0.5f), Offset(w * 0.66f, h * 0.5f), strokeWidth = w * 0.05f)
            }
            "switch" -> {
                drawRoundRect(color.copy(alpha = 0.75f), Offset(w * 0.02f, h * 0.3f), Size(w * 0.96f, h * 0.4f), CornerRadius(h * 0.2f))
                drawCircle(Color.White, radius = h * 0.16f, center = Offset(w * 0.74f, h * 0.5f))
            }
            "checkbox" -> {
                drawRoundRect(soft, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), CornerRadius(w * 0.14f))
                drawLine(color, Offset(w * 0.32f, h * 0.52f), Offset(w * 0.45f, h * 0.66f), strokeWidth = w * 0.08f)
                drawLine(color, Offset(w * 0.45f, h * 0.66f), Offset(w * 0.7f, h * 0.36f), strokeWidth = w * 0.08f)
            }
            "slider" -> {
                drawLine(soft, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = h * 0.12f)
                drawLine(color, Offset(0f, h * 0.5f), Offset(w * 0.6f, h * 0.5f), strokeWidth = h * 0.12f)
                drawCircle(color, radius = h * 0.2f, center = Offset(w * 0.6f, h * 0.5f))
            }
            "list", "menu" -> {
                repeat(3) { index ->
                    val y = h * (0.24f + index * 0.26f)
                    drawCircle(color.copy(alpha = 0.55f), radius = w * 0.07f, center = Offset(w * 0.18f, y))
                    drawLine(color.copy(alpha = 0.45f), Offset(w * 0.32f, y), Offset(w * 0.85f, y), strokeWidth = h * 0.055f)
                }
            }
            "image", "video", "map", "chart", "canvas" -> {
                drawRoundRect(soft, Offset(0f, h * 0.12f), Size(w, h * 0.76f), CornerRadius(w * 0.1f))
                val path = Path().apply {
                    moveTo(0f, h * 0.88f)
                    lineTo(w * 0.3f, h * 0.42f)
                    lineTo(w * 0.52f, h * 0.66f)
                    lineTo(w * 0.72f, h * 0.46f)
                    lineTo(w, h * 0.88f)
                    close()
                }
                drawPath(path, color.copy(alpha = 0.75f))
            }
            "condition" -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    lineTo(w, h * 0.5f)
                    lineTo(w * 0.5f, h)
                    lineTo(0f, h * 0.5f)
                    close()
                }
                drawPath(path, soft)
                drawPath(path, color, style = Stroke(width = 1.4f))
            }
            else -> {
                drawRoundRect(soft, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), CornerRadius(w * 0.18f))
                drawCircle(color, radius = w * 0.14f, center = center)
            }
        }
    }
}

/**
 * Keeps a modal sheet still while its scrollable content sits at a boundary.
 *
 * Material3 hands the drag deltas and fling velocity that the inner list can no
 * longer consume to the sheet itself, so a full-height sheet dips and snaps back
 * every time the user scrolls past the end of the content. Consuming that
 * leftover vertical input keeps the sheet in place and leaves dismissal to the
 * drag handle and the explicit buttons.
 */
@Composable
internal fun rememberSheetDragBlocker(): NestedScrollConnection = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset = if (source == NestedScrollSource.Drag) Offset(0f, available.y) else Offset.Zero

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            Velocity(0f, available.y)
    }
}
