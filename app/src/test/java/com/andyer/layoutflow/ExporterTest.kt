package com.andyer.layoutflow

import com.andyer.layoutflow.model.FlowEdge
import com.andyer.layoutflow.model.FlowNode
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.demoProject
import com.andyer.layoutflow.model.esc
import com.andyer.layoutflow.model.projectJson
import com.andyer.layoutflow.model.toJsonText
import com.andyer.layoutflow.model.toSvgText
import com.andyer.layoutflow.model.toXmlText
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExporterTest {

    private val sample = FlowProject(
        name = "Checkout <Flow> & \"Return\"",
        nodes = listOf(
            FlowNode(
                id = "n1",
                specId = "android.screen",
                name = "Cart & Checkout",
                platform = "android",
                kind = "screen",
                x = 10f,
                y = 20f,
                width = 280f,
                height = 500f,
                text = "Order <1>",
            ),
            FlowNode(
                id = "n2",
                specId = "web.button",
                name = "Pay",
                platform = "web",
                kind = "button",
                x = 340f,
                y = 200f,
                width = 110f,
                height = 40f,
                text = "Pay & Go",
            ),
        ),
        edges = listOf(
            FlowEdge(
                id = "e1",
                from = "n1",
                to = "n2",
                trigger = "click",
                action = "navigate",
                transition = "sharedAxisX",
                duration = 300,
                condition = "total > 0 && hasToken",
            ),
        ),
    )

    @Test
    fun escapesXmlSpecialCharacters() {
        assertEquals("&lt;&amp;&gt;&quot;&apos;", esc("<&>\"'"))
    }

    @Test
    fun xmlContainsEscapedNodesAndConnections() {
        val xml = sample.toXmlText()
        assertTrue(xml.startsWith("<?xml version=\"1.0\""))
        assertTrue(xml.contains("name=\"Cart &amp; Checkout\""))
        assertTrue(xml.contains("text=\"Order &lt;1&gt;\""))
        assertTrue(xml.contains("condition=\"total &gt; 0 &amp;&amp; hasToken\""))
        assertTrue(xml.contains("<connection id=\"e1\" from=\"n1\" to=\"n2\""))
        assertTrue(xml.contains("</layout-flow>"))
    }

    @Test
    fun jsonRoundTripKeepsProject() {
        val decoded = projectJson.decodeFromString<FlowProject>(sample.toJsonText())
        assertEquals(sample.name, decoded.name)
        assertEquals(sample.nodes.size, decoded.nodes.size)
        assertEquals(sample.edges.single().condition, decoded.edges.single().condition)
    }

    @Test
    fun svgContainsAllNodesAndEdges() {
        val svg = sample.toSvgText()
        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.endsWith("</svg>\n"))
        assertTrue(svg.contains("Cart &amp; Checkout"))
        assertTrue(svg.contains("Pay &amp; Go"))
        assertTrue(svg.contains("<path"))
        assertTrue(svg.contains("total &gt; 0"))
    }

    @Test
    fun demoProjectIsConnected() {
        val project = demoProject()
        assertTrue(project.nodes.size >= 6)
        assertTrue(project.edges.isNotEmpty())
        assertTrue(project.edges.all { edge ->
            project.nodes.any { it.id == edge.from } && project.nodes.any { it.id == edge.to }
        })
    }
}
