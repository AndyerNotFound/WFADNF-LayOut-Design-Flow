package com.andyer.layoutflow.data

import android.content.Context
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.projectJson
import com.andyer.layoutflow.model.toJsonText
import kotlinx.serialization.decodeFromString
import java.io.File

class ProjectStore(context: Context) {
    private val root = File(context.filesDir, "projects").apply { mkdirs() }
    private val autosaveFile = File(root, "autosave.flow.json")

    fun readAutosave(): FlowProject? = runCatching {
        if (autosaveFile.exists()) {
            projectJson.decodeFromString<FlowProject>(autosaveFile.readText())
        } else {
            null
        }
    }.getOrNull()

    fun writeAutosave(project: FlowProject) {
        root.mkdirs()
        autosaveFile.writeText(project.toJsonText())
    }

    fun parseProject(text: String): FlowProject = projectJson.decodeFromString(text)

    fun autosavePath(): String = autosaveFile.absolutePath
}
