package com.matalass.graphroutestudio.analysis;

import com.matalass.graphroutestudio.engine.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphAnalyzerTest {

    @Test
    void analyzeComputesCoreMetrics() {
        Graph graph = new Graph(false, true, "Metrics");
        graph.addNode(0, "A", 0, 0);
        graph.addNode(1, "B", 0, 0);
        graph.addNode(2, "C", 0, 0);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 4);

        GraphMetrics metrics = GraphAnalyzer.analyze(graph);

        assertEquals(3, metrics.nodeCount());
        assertEquals(2, metrics.edgeCount());
        assertTrue(metrics.connected());
        assertFalse(metrics.directed());
        assertEquals(6.0, metrics.totalWeight());
    }
}
