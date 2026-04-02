package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FordFulkerson {
    private FordFulkerson() {
    }

    public static AlgorithmResult run(Graph graph, int sourceId, int sinkId) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.MAX_FLOW, "Ford-Fulkerson (Edmonds-Karp)");
        if (graph.getNode(sourceId).isEmpty()) { result.setError("Source introuvable."); return result; }
        if (graph.getNode(sinkId).isEmpty()) { result.setError("Puits introuvable."); return result; }
        if (sourceId == sinkId) { result.setError("Source = puits."); return result; }

        List<Node> nodes = graph.nodeList();
        int n = nodes.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(nodes.get(i).getId(), i);
        }

        int[][] cap = new int[n][n];
        for (Edge edge : graph.getEdges()) {
            int i = idx.get(edge.getSource().getId());
            int j = idx.get(edge.getTarget().getId());
            cap[i][j] += Math.max(0, (int) Math.round(edge.getWeight()));
            if (!graph.isDirected()) {
                cap[j][i] += Math.max(0, (int) Math.round(edge.getWeight()));
            }
        }

        int src = idx.get(sourceId);
        int sink = idx.get(sinkId);
        int totalFlow = 0;
        int iteration = 0;
        int[] prev = new int[n];
        Map<Edge, Integer> flowMap = new HashMap<>();
        for (Edge edge : graph.getEdges()) {
            flowMap.put(edge, 0);
        }

        while (true) {
            Arrays.fill(prev, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(src);
            prev[src] = src;

            while (!queue.isEmpty() && prev[sink] == -1) {
                int u = queue.removeFirst();
                for (int v = 0; v < n; v++) {
                    if (prev[v] == -1 && cap[u][v] > 0) {
                        prev[v] = u;
                        queue.addLast(v);
                    }
                }
            }

            if (prev[sink] == -1) {
                break;
            }

            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != src; v = prev[v]) {
                int u = prev[v];
                pathFlow = Math.min(pathFlow, cap[u][v]);
            }

            iteration++;
            Set<Integer> pathNodeIds = new HashSet<>();
            for (int v = sink; v != src; v = prev[v]) {
                int u = prev[v];
                cap[u][v] -= pathFlow;
                cap[v][u] += pathFlow;
                pathNodeIds.add(nodes.get(u).getId());
                pathNodeIds.add(nodes.get(v).getId());
            }
            totalFlow += pathFlow;
            result.log("Itération " + iteration + " : flot ajouté = " + pathFlow + " | flot total = " + totalFlow);
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Chemin augmentant " + iteration + " — flot +" + pathFlow,
                    new HashSet<>(idx.keySet()),
                    pathNodeIds,
                    pathNodeIds,
                    Set.of(),
                    Map.of()
            ));
        }

        int[][] originalCap = new int[n][n];
        for (Edge edge : graph.getEdges()) {
            int i = idx.get(edge.getSource().getId());
            int j = idx.get(edge.getTarget().getId());
            originalCap[i][j] += Math.max(0, (int) Math.round(edge.getWeight()));
        }
        for (Edge edge : graph.getEdges()) {
            int i = idx.get(edge.getSource().getId());
            int j = idx.get(edge.getTarget().getId());
            flowMap.put(edge, Math.max(0, originalCap[i][j] - cap[i][j]));
        }

        result.setMaxFlow(totalFlow);
        result.setFlowMap(flowMap);
        result.addStep(new AlgorithmResult.AnimationStep(
                "Flot maximal = " + totalFlow,
                new HashSet<>(idx.keySet()),
                Set.of(sourceId, sinkId),
                Set.of(),
                Set.of(),
                Map.of()
        ));
        result.log("\nFlot maximal = " + totalFlow);
        return result;
    }
}
