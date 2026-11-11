package com.andyer.layoutflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andyer.layoutflow.model.ACTIONS
import com.andyer.layoutflow.model.EASINGS
import com.andyer.layoutflow.model.FlowEdge
import com.andyer.layoutflow.model.FlowNode
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.PLATFORMS
import com.andyer.layoutflow.model.TRANSITIONS
import com.andyer.layoutflow.model.TRIGGERS
import com.andyer.layoutflow.model.actionSpec
import com.andyer.layoutflow.model.autoLabel
import com.andyer.layoutflow.model.easingSpec
import com.andyer.layoutflow.model.nodeById
import com.andyer.layoutflow.model.platformInfo
import com.andyer.layoutflow.model.specForNode
import com.andyer.layoutflow.model.transitionSpec
import com.andyer.layoutflow.model.triggerSpec
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorSheet(
    project: FlowProject,
    node: FlowNode?,
    edge: FlowEdge?,
    vm: EditorViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (node != null) "元件属性" else "连线属性",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("完成", fontSize = 11.sp) }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (node != null) {
                    item { NodeHeader(node) }
                    item { NodeFields(node, vm) }
                } else if (edge != null) {
                    item { EdgeHeader(project, edge) }
                    item { EdgeFields(edge, vm) }
                }
            }
        }
    }
}

@Composable
private fun NodeHeader(node: FlowNode) {
    val spec = specForNode(node)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(node.name.ifBlank { spec.name }, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${platformInfo(node.platform).label} · ${spec.name} · ${node.kind}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${node.width.roundToInt()} × ${node.height.roundToInt()} dp",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
    if (spec.description.isNotBlank()) {
        Text(
            spec.description,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NodeFields(node: FlowNode, vm: EditorViewModel) {
    SectionTitle("基本信息")
    OutlinedTextField(
        value = node.name,
        onValueChange = { value ->
            vm.editNode(node.id, "node:${node.id}:name") { it.copy(name = value) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("元件名称", fontSize = 11.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    OutlinedTextField(
        value = node.text,
        onValueChange = { value ->
            vm.editNode(node.id, "node:${node.id}:text") { it.copy(text = value) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("显示文本 / 数据", fontSize = 11.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Text("平台", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(PLATFORMS, key = { it.id }) { platform ->
            FilterChip(
                selected = node.platform == platform.id,
                onClick = {
                    vm.editNode(node.id, "node:${node.id}:platform") { it.copy(platform = platform.id) }
                },
                label = { Text(platform.label, fontSize = 10.sp) },
            )
        }
    }

    SectionTitle("位置与尺寸")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("X", node.x, Modifier.weight(1f)) { value ->
            vm.editNode(node.id, "node:${node.id}:x") { it.copy(x = value) }
        }
        NumberField("Y", node.y, Modifier.weight(1f)) { value ->
            vm.editNode(node.id, "node:${node.id}:y") { it.copy(y = value) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("宽", node.width, Modifier.weight(1f)) { value ->
            vm.editNode(node.id, "node:${node.id}:w") { it.copy(width = value.coerceAtLeast(40f)) }
        }
        NumberField("高", node.height, Modifier.weight(1f)) { value ->
            vm.editNode(node.id, "node:${node.id}:h") { it.copy(height = value.coerceAtLeast(24f)) }
        }
    }

    SectionTitle("外观")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("圆角 dp", node.corner, Modifier.weight(1f)) { value ->
            vm.editNode(node.id, "node:${node.id}:corner") { it.copy(corner = value.coerceIn(0f, 80f)) }
        }
        NumberField("高度 dp", node.elevation, Modifier.weight(1f)) { value ->
            vm.editNode(node.id, "node:${node.id}:elevation") { it.copy(elevation = value.coerceIn(0f, 24f)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = node.accent,
            onValueChange = { value ->
                vm.editNode(node.id, "node:${node.id}:accent") { it.copy(accent = value) }
            },
            modifier = Modifier.weight(1f),
            label = { Text("强调色 #RRGGBB", fontSize = 10.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("默认", fontSize = 11.sp) },
        )
        NumberField("不透明度 %", node.opacity * 100f, Modifier.weight(0.7f)) { value ->
            vm.editNode(node.id, "node:${node.id}:opacity") { it.copy(opacity = (value / 100f).coerceIn(0.1f, 1f)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (hex in listOf("#6750A4", "#7D5260", "#B3261E", "#006A6A", "#0061A4", "#8A5100", "#4F5B66")) {
            val color = accentColor(hex)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(color, CircleShape)
                    .border(
                        if (node.accent.equals(hex, true)) 2.dp else 1.dp,
                        if (node.accent.equals(hex, true)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
                    .clickable {
                        vm.editNode(node.id, "node:${node.id}:accent") { it.copy(accent = hex) }
                    },
            )
        }
    }

    SectionTitle("附加属性")
    KeyValueEditor(
        nodeId = node.id,
        initial = node.properties,
        hint = "每行 key=value，用于记录业务数据、平台属性或绑定信息",
    ) { map ->
        vm.editNode(node.id, "node:${node.id}:properties") { it.copy(properties = map) }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { vm.duplicateNode(node.id) },
            modifier = Modifier.weight(1f),
        ) { Text("复制元件", fontSize = 11.sp) }
        OutlinedButton(
            onClick = { vm.deleteNode(node.id) },
            modifier = Modifier.weight(1f),
        ) {
            Text("删除元件", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun EdgeHeader(project: FlowProject, edge: FlowEdge) {
    val from = project.nodeById(edge.from)
    val to = project.nodeById(edge.to)
    Column {
        Text("连线", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "${from?.name ?: edge.from} → ${to?.name ?: edge.to}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EdgeFields(edge: FlowEdge, vm: EditorViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            edge.autoLabel(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    SectionTitle("触发与结果")
    ChoiceField(
        label = "触发方式 Trigger",
        value = triggerSpec(edge.trigger).label,
        options = TRIGGERS.map { it.id to it.label },
    ) { value -> vm.editEdge(edge.id, "edge:${edge.id}:trigger") { it.copy(trigger = value) } }
    ChoiceField(
        label = "执行动作 Action",
        value = actionSpec(edge.action).label,
        options = ACTIONS.map { it.id to it.label },
    ) { value -> vm.editEdge(edge.id, "edge:${edge.id}:action") { it.copy(action = value) } }

    SectionTitle("视图过渡")
    ChoiceField(
        label = "过渡动画 Transition",
        value = transitionSpec(edge.transition).label,
        options = TRANSITIONS.map { it.id to it.label },
    ) { value -> vm.editEdge(edge.id, "edge:${edge.id}:transition") { it.copy(transition = value) } }
    Text(
        transitionSpec(edge.transition).hint,
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IntField("时长 ms", edge.duration, Modifier.weight(1f)) { value ->
            vm.editEdge(edge.id, "edge:${edge.id}:duration") { it.copy(duration = value.coerceIn(0, 5000)) }
        }
        IntField("延迟 ms", edge.delay, Modifier.weight(1f)) { value ->
            vm.editEdge(edge.id, "edge:${edge.id}:delay") { it.copy(delay = value.coerceIn(0, 10000)) }
        }
    }
    ChoiceField(
        label = "缓动曲线 Easing",
        value = easingSpec(edge.easing).label,
        options = EASINGS.map { it.id to it.label },
    ) { value -> vm.editEdge(edge.id, "edge:${edge.id}:easing") { it.copy(easing = value) } }

    SectionTitle("条件与标注")
    OutlinedTextField(
        value = edge.condition,
        onValueChange = { value ->
            vm.editEdge(edge.id, "edge:${edge.id}:condition") { it.copy(condition = value) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("条件表达式（可选）", fontSize = 11.sp) },
        placeholder = { Text("例如：token != null", fontSize = 11.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    OutlinedTextField(
        value = edge.label,
        onValueChange = { value ->
            vm.editEdge(edge.id, "edge:${edge.id}:label") { it.copy(label = value) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("自定义连线文案（留空自动生成）", fontSize = 11.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )

    SectionTitle("附加属性")
    KeyValueEditor(
        nodeId = edge.id,
        initial = edge.properties,
        hint = "每行 key=value，例如 analytics=login_click",
    ) { map ->
        vm.editEdge(edge.id, "edge:${edge.id}:properties") { it.copy(properties = map) }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { vm.reverseEdge(edge.id) },
            modifier = Modifier.weight(1f),
        ) { Text("交换方向", fontSize = 11.sp) }
        OutlinedButton(
            onClick = { vm.deleteEdge(edge.id) },
            modifier = Modifier.weight(1f),
        ) {
            Text("删除连线", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun NumberField(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
    onValue: (Float) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(formatNumber(value)) }
    LaunchedEffect(value, focused) {
        if (!focused) text = formatNumber(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.toFloatOrNull()?.let(onValue)
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        label = { Text(label, fontSize = 9.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun IntField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onValue: (Int) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value, focused) {
        if (!focused) text = value.toString()
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.toIntOrNull()?.let(onValue)
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        label = { Text(label, fontSize = 9.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun ChoiceField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, scheme.outline, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(label, fontSize = 9.sp, color = scheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("▾", fontSize = 12.sp, color = scheme.primary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(option.second, fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        onSelect(option.first)
                    },
                )
            }
        }
    }
}

@Composable
private fun KeyValueEditor(
    nodeId: String,
    initial: Map<String, String>,
    hint: String,
    onApply: (Map<String, String>) -> Unit,
) {
    var text by remember(nodeId) {
        mutableStateOf(initial.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("属性（key=value）", fontSize = 10.sp) },
            placeholder = { Text(hint, fontSize = 10.sp) },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = {
                val map = buildMap {
                    text.lineSequence().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) return@forEach
                        val index = trimmed.indexOf('=')
                        if (index > 0) {
                            put(trimmed.substring(0, index).trim(), trimmed.substring(index + 1).trim())
                        }
                    }
                }
                onApply(map)
            }) { Text("应用属性", fontSize = 10.sp) }
        }
    }
}

private fun formatNumber(value: Float): String {
    return if (abs(value - value.roundToInt()) < 0.01f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
