package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphStructureAlgorithmsTest {

    @Test
    void kruskalReturnsMinimumSpanningTreeWeight() {
        Graph graph = new Graph(false, true, "MST");
        graph.addNode(0, "A", 0, 0);
        graph.addNode(1, "B", 0, 0);
        graph.addNode(2, "C", 0, 0);
        graph.addNode(3, "D", 0, 0);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 3, 4);
        graph.addEdge(2, 3, 5);

        AlgorithmResult result = KruskalPrim.kruskal(graph);

        assertTrue(result.isSuccess());
        assertEquals(7.0, result.getSpanningTreeWeight());
        assertEquals(3, result.getSpanningTreeEdges().size());
    }

    @Test
    void cycleDetectorDetectsSimpleCycle() {
        Graph graph = new Graph(true, true, "Cycle");
        graph.addNode(0, "A", 0, 0);
        graph.addNode(1, "B", 0, 0);
        graph.addNode(2, "C", 0, 0);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);

        AlgorithmResult result = CycleDetector.run(graph);

        assertTrue(result.isSuccess());
        assertTrue(result.hasCycle());
    }
}
