package com.andyer.layoutflow.ui

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andyer.layoutflow.data.ProjectStore
import com.andyer.layoutflow.model.ComponentSpec
import com.andyer.layoutflow.model.FlowEdge
import com.andyer.layoutflow.model.FlowNode
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.bounds
import com.andyer.layoutflow.model.demoProject
import com.andyer.layoutflow.model.nodeById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class EditorUiState(
    val project: FlowProject = FlowProject(),
    val selectedNodeId: String? = null,
    val selectedEdgeId: String? = null,
    val inspectRequested: Boolean = false,
    val scale: Float = 1f,
    val pan: Offset = Offset.Zero,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saving: Boolean = false,
    val savedAt: Long = 0L,
    val message: String? = null,
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProjectStore(application)
    private val undoStack = ArrayDeque<FlowProject>()
    private val redoStack = ArrayDeque<FlowProject>()
    private var lastUndoKey: String? = null
    private var lastUndoAt = 0L
    private var saveJob: Job? = null

    private val restored = store.readAutosave()
    private val initialProject = restored ?: demoProject()

    private val _state = MutableStateFlow(
        EditorUiState(
            project = initialProject,
            scale = 1f,
            pan = Offset(48f, 32f),
            savedAt = if (restored != null) System.currentTimeMillis() else 0L,
        ),
    )
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private fun publish(next: FlowProject) {
        val current = _state.value
        val selectedNode = current.selectedNodeId?.takeIf { next.nodeById(it) != null }
        val selectedEdge = current.selectedEdgeId?.takeIf { id -> next.edges.any { it.id == id } }
        _state.value = current.copy(
            project = next.copy(updatedAt = System.currentTimeMillis()),
            selectedNodeId = selectedNode,
            selectedEdgeId = selectedEdge,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            saving = true,
        )
        scheduleSave()
    }

    private fun record(key: String? = null) {
        val now = System.currentTimeMillis()
        if (key != null && key == lastUndoKey && now - lastUndoAt < 900L) {
            lastUndoAt = now
            return
        }
        undoStack.addLast(_state.value.project)
        while (undoStack.size > 80) undoStack.removeFirst()
        redoStack.clear()
        lastUndoKey = key
        lastUndoAt = now
        _state.value = _state.value.copy(canUndo = true, canRedo = false)
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            val project = _state.value.project
            withContext(Dispatchers.IO) { store.writeAutosave(project) }
            _state.value = _state.value.copy(saving = false, savedAt = System.currentTimeMillis())
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        val project = _state.value.project
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.writeAutosave(project) }
            _state.value = _state.value.copy(saving = false, savedAt = System.currentTimeMillis())
        }
    }

    fun setMessage(message: String) {
        _state.value = _state.value.copy(message = message)
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun selectNode(id: String?) {
        _state.value = _state.value.copy(
            selectedNodeId = id,
            selectedEdgeId = null,
            inspectRequested = id != null,
        )
    }

    fun selectEdge(id: String?) {
        _state.value = _state.value.copy(
            selectedEdgeId = id,
            selectedNodeId = null,
            inspectRequested = id != null,
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(
            selectedNodeId = null,
            selectedEdgeId = null,
            inspectRequested = false,
        )
    }

    fun renameProject(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || clean == _state.value.project.name) return
        record("project:name")
        publish(_state.value.project.copy(name = clean))
    }

    fun toggleSnap() {
        record("project:snap")
        publish(_state.value.project.copy(snapToGrid = !_state.value.project.snapToGrid))
    }

    fun newProject() {
        record("project:new")
        publish(FlowProject(name = "未命名流程"))
        _state.value = _state.value.copy(scale = 1f, pan = Offset(48f, 32f), inspectRequested = false)
    }

    fun openProjectText(text: String): Boolean {
        return runCatching {
            val parsed = store.parseProject(text)
            val normalized = normalize(parsed)
            record("project:open")
            publish(normalized)
            _state.value = _state.value.copy(scale = 1f, pan = Offset(48f, 32f), inspectRequested = false)
            true
        }.getOrElse {
            setMessage("无法读取项目：${it.message ?: "格式不正确"}")
            false
        }
    }

    private fun normalize(project: FlowProject): FlowProject {
        val maxNode = project.nodes.mapNotNull { it.id.removePrefix("n").toIntOrNull() }.maxOrNull() ?: 0
        val maxEdge = project.edges.mapNotNull { it.id.removePrefix("e").toIntOrNull() }.maxOrNull() ?: 0
        val safeNodes = project.nodes.map { node ->
            node.copy(
                width = node.width.coerceIn(24f, 4000f),
                height = node.height.coerceIn(16f, 4000f),
                corner = node.corner.coerceIn(0f, 120f),
                elevation = node.elevation.coerceIn(0f, 24f),
                opacity = node.opacity.coerceIn(0.1f, 1f),
            )
        }
        return project.copy(
            nodes = safeNodes,
            nextNodeSeq = max(project.nextNodeSeq, maxNode + 1),
            nextEdgeSeq = max(project.nextEdgeSeq, maxEdge + 1),
        )
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.project)
        lastUndoKey = null
        _state.value = _state.value.copy(project = previous, scale = _state.value.scale)
        publish(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.project)
        lastUndoKey = null
        publish(next)
    }

    fun addNode(spec: ComponentSpec, centerWorld: Offset) {
        val project = _state.value.project
        val seq = project.nextNodeSeq
        val grid = project.gridSize.toFloat()
        var x = centerWorld.x - spec.width / 2f
        var y = centerWorld.y - spec.height / 2f
        if (project.snapToGrid) {
            x = (x / grid).roundToInt() * grid
            y = (y / grid).roundToInt() * grid
        }
        val node = FlowNode(
            id = "n$seq",
            specId = spec.id,
            name = spec.name,
            platform = spec.platform,
            kind = spec.kind,
            x = x,
            y = y,
            width = spec.width,
            height = spec.height,
            text = spec.text,
            corner = if (spec.kind == "screen") 24f else 16f,
            elevation = if (spec.kind == "screen") 2f else 0f,
        )
        record("node:add")
        publish(
            project.copy(
                nodes = project.nodes + node,
                nextNodeSeq = seq + 1,
            ),
        )
        _state.value = _state.value.copy(
            selectedNodeId = node.id,
            selectedEdgeId = null,
            inspectRequested = false,
        )
    }

    fun duplicateNode(id: String) {
        val project = _state.value.project
        val node = project.nodeById(id) ?: return
        val seq = project.nextNodeSeq
        val copy = node.copy(
            id = "n$seq",
            name = "${node.name} 副本",
            x = node.x + 24f,
            y = node.y + 24f,
        )
        record("node:duplicate")
        publish(project.copy(nodes = project.nodes + copy, nextNodeSeq = seq + 1))
        _state.value = _state.value.copy(
            selectedNodeId = copy.id,
            selectedEdgeId = null,
            inspectRequested = false,
        )
    }

    fun deleteNode(id: String) {
        val project = _state.value.project
        if (project.nodeById(id) == null) return
        record("node:delete")
        publish(
            project.copy(
                nodes = project.nodes.filterNot { it.id == id },
                edges = project.edges.filterNot { it.from == id || it.to == id },
            ),
        )
        _state.value = _state.value.copy(selectedNodeId = null, inspectRequested = false)
    }

    fun beginNodeDrag(id: String) {
        _state.value = _state.value.copy(
            selectedNodeId = id,
            selectedEdgeId = null,
            inspectRequested = false,
        )
        record("node:drag:$id")
    }

    fun moveNodeBy(id: String, dx: Float, dy: Float) {
        val project = _state.value.project
        val node = project.nodeById(id) ?: return
        val next = project.copy(nodes = project.nodes.map {
            if (it.id == id) it.copy(x = it.x + dx, y = it.y + dy) else it
        })
        _state.value = _state.value.copy(project = next, saving = true)
    }

    fun endNodeDrag(id: String) {
        lastUndoKey = null
        val project = _state.value.project
        val node = project.nodeById(id) ?: return
        if (!project.snapToGrid) {
            scheduleSave()
            return
        }
        val grid = project.gridSize.toFloat()
        val snapped = node.copy(
            x = (node.x / grid).roundToInt() * grid,
            y = (node.y / grid).roundToInt() * grid,
        )
        if (snapped == node) {
            scheduleSave()
            return
        }
        _state.value = _state.value.copy(
            project = project.copy(nodes = project.nodes.map { if (it.id == id) snapped else it }),
            saving = true,
        )
        scheduleSave()
    }

    fun beginNodeResize(id: String) {
        _state.value = _state.value.copy(
            selectedNodeId = id,
            selectedEdgeId = null,
            inspectRequested = false,
        )
        record("node:resize:$id")
    }

    fun resizeNodeBy(id: String, dx: Float, dy: Float) {
        val project = _state.value.project
        val node = project.nodeById(id) ?: return
        val next = project.copy(nodes = project.nodes.map {
            if (it.id == id) {
                it.copy(
                    width = (it.width + dx).coerceAtLeast(48f),
                    height = (it.height + dy).coerceAtLeast(32f),
                )
            } else {
                it
            }
        })
        _state.value = _state.value.copy(project = next, saving = true)
    }

    fun editNode(id: String, undoKey: String?, transform: (FlowNode) -> FlowNode) {
        val project = _state.value.project
        val node = project.nodeById(id) ?: return
        val updated = transform(node)
        if (updated == node) return
        record(undoKey)
        publish(project.copy(nodes = project.nodes.map { if (it.id == id) updated else it }))
    }

    fun addEdge(from: String, to: String): String? {
        if (from == to) return null
        val project = _state.value.project
        if (project.nodeById(from) == null || project.nodeById(to) == null) return null
        if (project.edges.any { it.from == from && it.to == to }) {
            setMessage("这两个元件之间已经有连线")
            return null
        }
        val target = project.nodeById(to)
        val action = when (target?.kind) {
            "screen" -> "navigate"
            "dialog" -> "openDialog"
            "sheet" -> "openSheet"
            else -> "updateState"
        }
        val transition = if (action == "navigate") "sharedAxisX" else "fade"
        val seq = project.nextEdgeSeq
        val edge = FlowEdge(
            id = "e$seq",
            from = from,
            to = to,
            action = action,
            transition = transition,
        )
        record("edge:add")
        publish(project.copy(edges = project.edges + edge, nextEdgeSeq = seq + 1))
        _state.value = _state.value.copy(
            selectedEdgeId = edge.id,
            selectedNodeId = null,
            inspectRequested = true,
        )
        return edge.id
    }

    fun deleteEdge(id: String) {
        val project = _state.value.project
        if (project.edges.none { it.id == id }) return
        record("edge:delete")
        publish(project.copy(edges = project.edges.filterNot { it.id == id }))
        _state.value = _state.value.copy(selectedEdgeId = null, inspectRequested = false)
    }

    fun editEdge(id: String, undoKey: String?, transform: (FlowEdge) -> FlowEdge) {
        val project = _state.value.project
        val edge = project.edges.firstOrNull { it.id == id } ?: return
        val updated = transform(edge)
        if (updated == edge) return
        record(undoKey)
        publish(project.copy(edges = project.edges.map { if (it.id == id) updated else it }))
    }

    fun reverseEdge(id: String) {
        val project = _state.value.project
        val edge = project.edges.firstOrNull { it.id == id } ?: return
        record("edge:reverse")
        publish(
            project.copy(
                edges = project.edges.map {
                    if (it.id == id) it.copy(from = edge.to, to = edge.from) else it
                },
            ),
        )
    }

    fun transformViewport(centroid: Offset, panDelta: Offset, zoomDelta: Float) {
        val current = _state.value
        val newScale = (current.scale * zoomDelta).coerceIn(0.2f, 3.5f)
        val worldX = (centroid.x - current.pan.x) / current.scale
        val worldY = (centroid.y - current.pan.y) / current.scale
        _state.value = current.copy(
            scale = newScale,
            pan = Offset(
                centroid.x - worldX * newScale + panDelta.x,
                centroid.y - worldY * newScale + panDelta.y,
            ),
        )
    }

    fun panBy(delta: Offset) {
        _state.value = _state.value.copy(pan = _state.value.pan + delta)
    }

    fun zoomAt(factor: Float, focus: Offset) {
        transformViewport(focus, Offset.Zero, factor)
    }

    fun setView(scale: Float, pan: Offset) {
        _state.value = _state.value.copy(scale = scale.coerceIn(0.2f, 3.5f), pan = pan)
    }

    fun fitToContent(viewportWidth: Float, viewportHeight: Float, density: Float) {
        val project = _state.value.project
        val b = project.bounds() ?: return
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        val pad = 48f * density
        val scale = min(
            (viewportWidth - pad * 2f) / (b.width * density).coerceAtLeast(1f),
            (viewportHeight - pad * 2f) / (b.height * density).coerceAtLeast(1f),
        ).coerceIn(0.2f, 2.4f)
        val centerX = (b.left + b.width / 2f) * density * scale
        val centerY = (b.top + b.height / 2f) * density * scale
        _state.value = _state.value.copy(
            scale = scale,
            pan = Offset(viewportWidth / 2f - centerX, viewportHeight / 2f - centerY),
        )
    }
}
