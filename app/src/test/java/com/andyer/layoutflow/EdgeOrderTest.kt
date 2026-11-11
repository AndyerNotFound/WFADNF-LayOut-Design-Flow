package com.andyer.layoutflow

import com.andyer.layoutflow.model.FlowEdge
import com.andyer.layoutflow.model.FlowNode
import com.andyer.layoutflow.model.FlowProject
import com.andyer.layoutflow.model.orderedEdges
import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeOrderTest {

    private fun node(id: String) = FlowNode(
        id = id,
        specId = "android.screen",
        name = id,
        platform = "android",
        kind = "screen",
        x = 0f,
        y = 0f,
        width = 100f,
        height = 100f,
    )

    private fun edge(id: String, from: String, to: String) = FlowEdge(id = id, from = from, to = to)

    private val project = FlowProject(
        nodes = listOf(node("n1"), node("n2"), node("n3")),
        edges = listOf(
            edge("e1", "n1", "n2"),
            edge("e2", "n1", "n3"),
            edge("e3", "n2", "n3"),
        ),
    )

    @Test
    fun edgesFollowTheFrontEndpointLayer() {
        assertEquals(listOf("e1", "e2", "e3"), project.orderedEdges().map { it.id })
    }

    @Test
    fun selectedEdgeIsDrawnLast() {
        assertEquals(listOf("e2", "e3", "e1"), project.orderedEdges("e1").map { it.id })
    }

    @Test
    fun equalDepthKeepsInsertionOrder() {
        val project = project.copy(
            edges = listOf(edge("a", "n1", "n2"), edge("b", "n1", "n2")),
        )
        assertEquals(listOf("a", "b"), project.orderedEdges().map { it.id })
    }

    @Test
    fun danglingEndpointsSinkToTheBack() {
        val dangling = project.copy(edges = project.edges + edge("e9", "n1", "ghost"))
        assertEquals(listOf("e9", "e1", "e2", "e3"), dangling.orderedEdges().map { it.id })
    }
}
