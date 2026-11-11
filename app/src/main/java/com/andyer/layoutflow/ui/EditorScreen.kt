package com.andyer.layoutflow.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andyer.layoutflow.BuildConfig
import com.andyer.layoutflow.model.ComponentSpec
import com.andyer.layoutflow.model.componentSpec
import com.andyer.layoutflow.model.nodeById
import com.andyer.layoutflow.model.platformInfo
import com.andyer.layoutflow.model.toJsonText
import com.andyer.layoutflow.model.toSvgText
import com.andyer.layoutflow.model.toXmlText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.roundToInt

private data class TemplateDragState(val spec: ComponentSpec, val point: Offset)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val project = state.project

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var viewportOrigin by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var templateDrag by remember { mutableStateOf<TemplateDragState?>(null) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    var showCatalog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showNewConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    fun notify(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun fit() {
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            vm.fitToContent(viewportSize.width.toFloat(), viewportSize.height.toFloat(), density)
        }
    }

    fun canvasCenterWorld(): Offset {
        val cx = viewportSize.width / 2f
        val cy = viewportSize.height / 2f
        val snapshot = vm.state.value
        return Offset(
            (cx - snapshot.pan.x) / snapshot.scale / density,
            (cy - snapshot.pan.y) / snapshot.scale / density,
        )
    }

    fun addAtCenter(spec: ComponentSpec) {
        vm.addNode(spec, canvasCenterWorld())
        notify("已添加 ${spec.name}")
    }

    val pngLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bitmap = withContext(Dispatchers.Default) {
                        DiagramRenderer.render(vm.state.value.project)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                    }
                    bitmap.recycle()
                }.onSuccess {
                    notify("已导出交互流程全图 PNG")
                }.onFailure {
                    notify("导出失败：${it.message ?: "未知错误"}")
                }
            }
        }
    }
    val svgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/svg+xml"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    writeText(context, uri, vm.state.value.project.toSvgText())
                }.onSuccess { notify("已导出 SVG 全图") }
                    .onFailure { notify("导出失败：${it.message ?: "未知错误"}") }
            }
        }
    }
    val xmlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/xml"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    writeText(context, uri, vm.state.value.project.toXmlText())
                }.onSuccess { notify("已导出交互流程 XML") }
                    .onFailure { notify("导出失败：${it.message ?: "未知错误"}") }
            }
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    writeText(context, uri, vm.state.value.project.toJsonText())
                }.onSuccess { notify("已导出项目 JSON") }
                    .onFailure { notify("导出失败：${it.message ?: "未知错误"}") }
            }
        }
    }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (text != null && vm.openProjectText(text)) {
                        delay(120)
                        fit()
                        notify("项目已导入")
                    }
                }.onFailure {
                    notify("导入失败：${it.message ?: "无法读取文件"}")
                }
            }
        }
    }

    fun fileName(extension: String): String {
        val base = vm.state.value.project.name
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .take(48)
            .ifBlank { "layout-flow" }
        return "$base.$extension"
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    var didInitialFit by remember { mutableStateOf(false) }
    LaunchedEffect(viewportSize) {
        if (!didInitialFit && viewportSize.width > 0 && viewportSize.height > 0) {
            didInitialFit = true
            fit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChangedCompat { rootSize = it },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                project.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                when {
                                    state.saving -> "正在保存…"
                                    state.savedAt > 0 -> "已自动保存"
                                    else -> "本地草稿"
                                },
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = vm::undo, enabled = state.canUndo) {
                            Text("↶", fontSize = 19.sp)
                        }
                        TextButton(onClick = vm::redo, enabled = state.canRedo) {
                            Text("↷", fontSize = 19.sp)
                        }
                        Box {
                            TextButton(onClick = { showMenu = true }) {
                                Text("⋮", fontSize = 20.sp)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("新建流程") },
                                    onClick = {
                                        showMenu = false
                                        showNewConfirm = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("重命名流程") },
                                    onClick = {
                                        showMenu = false
                                        showRename = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导入项目 JSON") },
                                    onClick = {
                                        showMenu = false
                                        openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("保存草稿") },
                                    onClick = {
                                        showMenu = false
                                        vm.saveNow()
                                        notify("草稿已保存")
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("导出交互流程全图 PNG") },
                                    onClick = {
                                        showMenu = false
                                        pngLauncher.launch(fileName("png"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导出矢量全图 SVG") },
                                    onClick = {
                                        showMenu = false
                                        svgLauncher.launch(fileName("svg"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导出交互流程 XML") },
                                    onClick = {
                                        showMenu = false
                                        xmlLauncher.launch(fileName("xml"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导出项目 JSON") },
                                    onClick = {
                                        showMenu = false
                                        jsonLauncher.launch(fileName("flow.json"))
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (project.snapToGrid) "网格吸附：开" else "网格吸附：关",
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        vm.toggleSnap()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("适应画布") },
                                    onClick = {
                                        showMenu = false
                                        fit()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("关于 LayOut Flow") },
                                    onClick = {
                                        showMenu = false
                                        showAbout = true
                                    },
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                PaletteShelf(
                    onOpenCatalog = { showCatalog = true },
                    onDragStart = { spec, point ->
                        templateDrag = TemplateDragState(spec, point)
                    },
                    onDragMove = { point ->
                        templateDrag = templateDrag?.copy(point = point)
                    },
                    onDragEnd = { point ->
                        val drag = templateDrag
                        templateDrag = null
                        if (drag != null && point != null) {
                            val local = point - viewportOrigin
                            if (local.x >= 0f && local.y >= 0f &&
                                local.x <= viewportSize.width && local.y <= viewportSize.height
                            ) {
                                val snapshot = vm.state.value
                                val world = Offset(
                                    (local.x - snapshot.pan.x) / snapshot.scale / density,
                                    (local.y - snapshot.pan.y) / snapshot.scale / density,
                                )
                                vm.addNode(drag.spec, world)
                                notify("已添加 ${drag.spec.name}")
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = 8.dp)) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .onGloballyPositioned { viewportOrigin = it.positionInRoot() },
            ) {
                FlowCanvas(
                    vm = vm,
                    state = state,
                    onNodeDragStart = { id ->
                        draggingNodeId = id
                        dragPoint = null
                    },
                    onNodeDragPoint = { point ->
                        val rootPoint = viewportOrigin + point
                        dragPoint = if (rootPoint.y < 240f * density) rootPoint else null
                    },
                    onNodeDragEnd = { id, center ->
                        val rootPoint = viewportOrigin + center
                        if (isNearTrash(rootPoint, rootSize, density)) {
                            vm.deleteNode(id)
                            notify("已删除元件")
                        }
                        draggingNodeId = null
                        dragPoint = null
                    },
                    onViewportSize = { viewportSize = it },
                    onAddFirst = {
                        vm.addNode(componentSpec("android.screen"), canvasCenterWorld())
                    },
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Column {
                            ZoomButton("+") {
                                vm.zoomAt(
                                    1.25f,
                                    Offset(viewportSize.width / 2f, viewportSize.height / 2f),
                                )
                            }
                            HorizontalDivider()
                            ZoomButton("−") {
                                vm.zoomAt(
                                    0.8f,
                                    Offset(viewportSize.width / 2f, viewportSize.height / 2f),
                                )
                            }
                            HorizontalDivider()
                            ZoomButton("适") { fit() }
                        }
                    }
                }
            }
        }

        val drag = templateDrag
        if (drag != null) {
            val platform = platformInfo(drag.spec.platform)
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (drag.point.x - 62.dp.value * density).roundToInt(),
                            (drag.point.y - 24.dp.value * density).roundToInt(),
                        )
                    }
                    .size(124.dp, 48.dp)
                    .graphicsLayer {
                        alpha = 0.94f
                        scaleX = 1.03f
                        scaleY = 1.03f
                    },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Color(platform.color.toInt()), CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        drag.spec.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (draggingNodeId != null) {
            val active = dragPoint?.let { isNearTrash(it, rootSize, density) } ?: false
            val scheme = MaterialTheme.colorScheme
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .size(76.dp),
                shape = CircleShape,
                color = if (active) scheme.errorContainer else scheme.surfaceContainerHigh,
                contentColor = if (active) scheme.onErrorContainer else scheme.onSurfaceVariant,
                border = BorderStroke(2.dp, if (active) scheme.error else scheme.outlineVariant),
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("删除", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("拖到这里", fontSize = 8.sp)
                }
            }
        }
    }

    if (showCatalog) {
        CatalogSheet(
            onDismiss = { showCatalog = false },
            onAdd = { spec ->
                showCatalog = false
                addAtCenter(spec)
            },
        )
    }

    if (state.inspectRequested && (state.selectedNodeId != null || state.selectedEdgeId != null)) {
        InspectorSheet(
            project = project,
            node = project.nodeById(state.selectedNodeId),
            edge = project.edges.firstOrNull { it.id == state.selectedEdgeId },
            vm = vm,
            onDismiss = { vm.clearSelection() },
        )
    }

    if (showRename) {
        var name by remember { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("重命名流程") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameProject(name)
                    showRename = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("取消") }
            },
        )
    }

    if (showNewConfirm) {
        AlertDialog(
            onDismissRequest = { showNewConfirm = false },
            title = { Text("新建流程") },
            text = { Text("当前画布会被清空，可以在之后用撤销恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.newProject()
                    showNewConfirm = false
                    vm.setView(1f, Offset(48f, 32f))
                }) { Text("新建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("LayOut Flow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("版本 ${BuildConfig.VERSION_NAME} · Material Design 3", fontSize = 11.sp)
                    Text(
                        "跨平台交互流程与前端视觉设计工具。拖拽元件、连接触发、设定视图过渡，并导出全图或结构化 XML。",
                        fontSize = 11.sp,
                    )
                    Text(
                        "长按元件库拖入画布；拖动元件右侧圆点连线；拖动选中元件的右下角调整尺寸；拖到顶部圆环删除。",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Modifier.onSizeChangedCompat(onSize: (IntSize) -> Unit): Modifier =
    this.then(Modifier.onSizeChanged(onSize))

private fun isNearTrash(point: Offset, root: IntSize, density: Float): Boolean {
    if (root.width == 0) return false
    val cx = root.width / 2f
    val cy = (96f + 38f) * density
    val radius = 54f * density
    val dx = point.x - cx
    val dy = point.y - cy
    return dx * dx + dy * dy <= radius * radius
}

private suspend fun writeText(context: Context, uri: Uri, text: String) {
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { stream: OutputStream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        }
    }
}
