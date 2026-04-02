package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortestPathAlgorithmsTest {

    @Test
    void dijkstraFindsExpectedShortestPath() {
        Graph graph = sampleGraph(false);

        AlgorithmResult result = Dijkstra.run(graph, 0, 3);

        assertTrue(result.isSuccess());
        assertEquals(4.0, result.getPathWeight());
        assertEquals("A", result.getPath().get(0).getLabel());
        assertEquals("D", result.getPath().get(result.getPath().size() - 1).getLabel());
    }

    @Test
    void bellmanFordHandlesNegativeEdgeWithoutNegativeCycle() {
        Graph graph = new Graph(true, true, "Bellman");
        graph.addNode(0, "S", 0, 0);
        graph.addNode(1, "A", 0, 0);
        graph.addNode(2, "B", 0, 0);
        graph.addNode(3, "T", 0, 0);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 5);
        graph.addEdge(1, 2, -2);
        graph.addEdge(2, 3, 3);
        graph.addEdge(1, 3, 8);

        AlgorithmResult result = BellmanFord.run(graph, 0, 3);

        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getPathWeight());
    }

    @Test
    void aStarFindsSameCostAsDijkstraOnTransportLikeGraph() {
        Graph graph = sampleGraph(false);

        AlgorithmResult dijkstra = Dijkstra.run(graph, 0, 4);
        AlgorithmResult astar = AStar.run(graph, 0, 4);

        assertTrue(dijkstra.isSuccess());
        assertTrue(astar.isSuccess());
        assertEquals(dijkstra.getPathWeight(), astar.getPathWeight());
    }

    private Graph sampleGraph(boolean directed) {
        Graph graph = new Graph(directed, true, "Sample");
        graph.addNode(0, "A", 0, 0);
        graph.addNode(1, "B", 0, 0);
        graph.addNode(2, "C", 0, 0);
        graph.addNode(3, "D", 0, 0);
        graph.addNode(4, "E", 0, 0);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 6);
        graph.addEdge(2, 3, 1);
        graph.addEdge(1, 3, 8);
        graph.addEdge(3, 4, 2);
        return graph;
    }
}
