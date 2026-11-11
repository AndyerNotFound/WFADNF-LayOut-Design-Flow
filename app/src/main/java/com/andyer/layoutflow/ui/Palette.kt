package com.andyer.layoutflow.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onTapSpec: (ComponentSpec) -> Unit,
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
                        onClick = { onTapSpec(spec) },
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
    onClick: () -> Unit,
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
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(Color(platform.color.toInt()), CircleShape),
                )
                Spacer(Modifier.width(4.dp))
                Text(platform.label, fontSize = 8.sp, color = scheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSheet(
    onDismiss: () -> Unit,
    onAdd: (ComponentSpec) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            Spacer(Modifier.height(6.dp))
            Text(
                spec.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
