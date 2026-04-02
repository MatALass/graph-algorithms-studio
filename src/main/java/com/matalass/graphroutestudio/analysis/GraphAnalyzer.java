package com.matalass.graphroutestudio.analysis;

import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class GraphAnalyzer {
    private GraphAnalyzer() {
    }

    public static GraphMetrics analyze(Graph graph) {
        int nodes = graph.nodeCount();
        int edges = graph.edgeCount();
        double density = computeDensity(graph);
        double averageDegree = nodes == 0 ? 0.0 : (graph.isDirected() ? (double) edges / nodes : (2.0 * edges) / nodes);
        return new GraphMetrics(
                nodes,
                edges,
                graph.isDirected(),
                graph.isWeighted(),
                isWeaklyConnected(graph),
                graph.hasNegativeWeights(),
                density,
                averageDegree,
                graph.totalWeight()
        );
    }

    private static double computeDensity(Graph graph) {
        int n = graph.nodeCount();
        int m = graph.edgeCount();
        if (n <= 1) {
            return 0.0;
        }
        if (graph.isDirected()) {
            return (double) m / (n * (n - 1));
        }
        return (2.0 * m) / (n * (n - 1));
    }

    private static boolean isWeaklyConnected(Graph graph) {
        if (graph.nodeCount() <= 1) {
            return true;
        }
        Node start = graph.getNodes().iterator().next();
        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start.getId());
        visited.add(start.getId());

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (Node neighbor : graph.neighbors(current)) {
                if (visited.add(neighbor.getId())) {
                    queue.addLast(neighbor.getId());
                }
            }
            if (graph.isDirected()) {
                for (Node node : graph.getNodes()) {
                    if (graph.neighbors(node.getId()).stream().anyMatch(n -> n.getId() == current) && visited.add(node.getId())) {
                        queue.addLast(node.getId());
                    }
                }
            }
        }
        return visited.size() == graph.nodeCount();
    }
}
