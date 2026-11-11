package com.andyer.layoutflow.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val projectJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun FlowProject.toJsonText(): String = projectJson.encodeToString(this)

fun FlowProject.toXmlText(): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<layout-flow version=\"1.0\" generator=\"LayOut Flow\" exported-at=\"${esc(stamp)}\">")
        appendLine("  <meta name=\"${esc(name)}\" nodes=\"${nodes.size}\" connections=\"${edges.size}\" grid=\"$gridSize\" snap=\"$snapToGrid\" />")
        appendLine("  <nodes>")
        for (node in nodes) {
            append("    <node id=\"${esc(node.id)}\" spec=\"${esc(node.specId)}\" name=\"${esc(node.name)}\"")
            append(" platform=\"${esc(node.platform)}\" kind=\"${esc(node.kind)}\"")
            append(" x=\"${fmt(node.x)}\" y=\"${fmt(node.y)}\" width=\"${fmt(node.width)}\" height=\"${fmt(node.height)}\"")
            append(" text=\"${esc(node.text)}\" accent=\"${esc(node.accent)}\" corner=\"${fmt(node.corner)}\"")
            append(" elevation=\"${fmt(node.elevation)}\" opacity=\"${fmt(node.opacity)}\">")
            if (node.properties.isEmpty()) {
                appendLine("</node>")
            } else {
                appendLine()
                for ((key, value) in node.properties) {
                    appendLine("      <property key=\"${esc(key)}\" value=\"${esc(value)}\" />")
                }
                appendLine("    </node>")
            }
        }
        appendLine("  </nodes>")
        appendLine("  <connections>")
        for (edge in edges) {
            append("    <connection id=\"${esc(edge.id)}\" from=\"${esc(edge.from)}\" to=\"${esc(edge.to)}\"")
            append(" trigger=\"${esc(edge.trigger)}\" action=\"${esc(edge.action)}\" transition=\"${esc(edge.transition)}\"")
            append(" duration=\"${edge.duration}\" delay=\"${edge.delay}\" easing=\"${esc(edge.easing)}\"")
            append(" condition=\"${esc(edge.condition)}\" label=\"${esc(edge.displayLabel())}\"")
            append(" from-name=\"${esc(nodeById(edge.from)?.name.orEmpty())}\" to-name=\"${esc(nodeById(edge.to)?.name.orEmpty())}\">")
            if (edge.properties.isEmpty()) {
                appendLine("</connection>")
            } else {
                appendLine()
                for ((key, value) in edge.properties) {
                    appendLine("      <property key=\"${esc(key)}\" value=\"${esc(value)}\" />")
                }
                appendLine("    </connection>")
            }
        }
        appendLine("  </connections>")
        appendLine("</layout-flow>")
    }
}

fun FlowProject.toSvgText(): String {
    val project = this
    val b = project.bounds() ?: return emptySvg(project.name)
    val padding = 40f
    val header = 64f
    val width = b.width + padding * 2f
    val height = b.height + header + padding * 2f
    val offsetX = padding - b.left
    val offsetY = header + padding - b.top
    val title = esc(project.name)
    val generated = esc(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${fmt(width)}\" height=\"${fmt(height)}\" viewBox=\"0 0 ${fmt(width)} ${fmt(height)}\">")
        appendLine("<rect width=\"100%\" height=\"100%\" fill=\"#FEF7FF\"/>")
        appendLine("<text x=\"24\" y=\"34\" font-family=\"sans-serif\" font-size=\"22\" font-weight=\"600\" fill=\"#1D1B20\">$title</text>")
        appendLine("<text x=\"24\" y=\"54\" font-family=\"sans-serif\" font-size=\"11\" fill=\"#49454F\">LayOut Flow · ${project.nodes.size} 个元件 · ${project.edges.size} 条连线 · $generated</text>")
        appendLine("<g transform=\"translate(${fmt(offsetX)},${fmt(offsetY)})\">")
        for (edge in project.edges) {
            val g = edgeGeometry(project, edge) ?: continue
            val color = if (edge.transition == "none") "#79747E" else "#6750A4"
            appendLine("<path d=\"M${fmt(g.startX)},${fmt(g.startY)} C${fmt(g.c1x)},${fmt(g.c1y)} ${fmt(g.c2x)},${fmt(g.c2y)} ${fmt(g.endX)},${fmt(g.endY)}\" fill=\"none\" stroke=\"$color\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>")
            val head = arrowHead(g.c2x, g.c2y, g.endX, g.endY, 10f, 8f)
            append("<polygon points=\"")
            append(head.joinToString(" ") { "${fmt(it.first)},${fmt(it.second)}" })
            appendLine("\" fill=\"$color\"/>")
            val (mx, my) = g.pointAt(0.5f)
            val text = esc(edge.displayLabel())
            val labelWidth = (text.length * 8.0f + 18f).coerceAtMost(260f)
            appendLine("<rect x=\"${fmt(mx - labelWidth / 2f)}\" y=\"${fmt(my - 10f)}\" width=\"${fmt(labelWidth)}\" height=\"20\" rx=\"10\" fill=\"#EADDFF\" stroke=\"#CAC4D0\" stroke-width=\"0.8\"/>")
            appendLine("<text x=\"${fmt(mx)}\" y=\"${fmt(my + 4f)}\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"9.5\" fill=\"#21005D\">$text</text>")
        }
        for (node in nodes) {
            val x = node.x
            val y = node.y
            val platform = platformInfo(node.platform)
            appendLine("<g>")
            appendLine("<rect x=\"${fmt(x)}\" y=\"${fmt(y)}\" width=\"${fmt(node.width)}\" height=\"${fmt(node.height)}\" rx=\"${fmt(node.corner)}\" fill=\"#F7F2FA\" stroke=\"#79747E\" stroke-width=\"1.2\"/>")
            appendLine("<rect x=\"${fmt(x + 10f)}\" y=\"${fmt(y + 10f)}\" width=\"24\" height=\"16\" rx=\"4\" fill=\"#${platform.color.toString(16).takeLast(6)}\"/>")
            appendLine("<text x=\"${fmt(x + 22f)}\" y=\"${fmt(y + 21.5f)}\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"9\" font-weight=\"700\" fill=\"#FFFFFF\">${esc(platform.short)}</text>")
            val nameChars = (node.width / 9.5f).roundToInt().coerceAtLeast(4)
            val textChars = (node.width / 8.5f).roundToInt().coerceAtLeast(4)
            appendLine("<text x=\"${fmt(x + 40f)}\" y=\"${fmt(y + 22f)}\" font-family=\"sans-serif\" font-size=\"10.5\" font-weight=\"600\" fill=\"#1D1B20\">${esc(ellipsizeSvg(node.name, nameChars))}</text>")
            appendLine("<text x=\"${fmt(x + 12f)}\" y=\"${fmt(y + 44f)}\" font-family=\"sans-serif\" font-size=\"15\" font-weight=\"500\" fill=\"#49454F\">${esc(ellipsizeSvg(node.text.ifBlank { node.name }, textChars))}</text>")
            val bodyTop = y + 54f
            val bodyHeight = (node.height - 62f).coerceAtLeast(8f)
            appendLine("<rect x=\"${fmt(x + 10f)}\" y=\"${fmt(bodyTop)}\" width=\"${fmt((node.width - 20f).coerceAtLeast(8f))}\" height=\"${fmt(bodyHeight)}\" rx=\"10\" fill=\"#ECE6F0\"/>")
            appendLine("<text x=\"${fmt(x + node.width / 2f)}\" y=\"${fmt(bodyTop + bodyHeight / 2f + 4f)}\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"11\" fill=\"#6750A4\">${esc(node.kind)}</text>")
            appendLine("</g>")
        }
        appendLine("</g>")
        appendLine("</svg>")
    }
}

private fun emptySvg(name: String): String = buildString {
    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"300\" viewBox=\"0 0 600 300\">")
    appendLine("<rect width=\"600\" height=\"300\" fill=\"#FEF7FF\"/>")
    appendLine("<text x=\"300\" y=\"140\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"22\" fill=\"#1D1B20\">${esc(name)}</text>")
    appendLine("<text x=\"300\" y=\"170\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"13\" fill=\"#79747E\">Empty flow</text>")
    appendLine("</svg>")
}

private fun ellipsizeSvg(value: String, maxChars: Int): String =
    if (value.length <= maxChars) value else value.take(maxChars - 1) + "…"

fun esc(value: String): String = buildString(value.length + 8) {
    for (ch in value) {
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            '\n' -> append("&#10;")
            else -> append(ch)
        }
    }
}

private fun fmt(value: Float): String {
    val rounded = Math.round(value * 10f) / 10f
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
}
