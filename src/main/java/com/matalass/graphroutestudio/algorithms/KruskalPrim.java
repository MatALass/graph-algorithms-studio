package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class KruskalPrim {
    private KruskalPrim() {
    }

    public static AlgorithmResult kruskal(Graph graph) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.SPANNING_TREE, "Kruskal");
        if (graph.nodeCount() == 0) { result.setError("Graphe vide."); return result; }
        if (graph.isDirected()) { result.setError("Kruskal s'applique à un graphe non orienté."); return result; }

        List<Edge> sorted = new ArrayList<>(graph.getEdges());
        sorted.sort(Comparator.comparingDouble(Edge::getWeight));
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, Integer> rank = new HashMap<>();
        for (Node node : graph.getNodes()) {
            parent.put(node.getId(), node.getId());
            rank.put(node.getId(), 0);
        }

        List<Edge> mst = new ArrayList<>();
        double totalWeight = 0.0;
        Set<Integer> mstNodes = new HashSet<>();
        Set<Integer> mstEdgeIds = new HashSet<>();
        result.log("=== Kruskal — Arbre couvrant minimal ===\n");

        for (Edge edge : sorted) {
            int u = edge.getSource().getId();
            int v = edge.getTarget().getId();
            int rootU = find(parent, u);
            int rootV = find(parent, v);
            if (rootU == rootV) {
                result.log("Arête " + edge.getSource().getLabel() + " - " + edge.getTarget().getLabel()
                        + " ignorée (cycle)");
                continue;
            }
            union(parent, rank, rootU, rootV);
            mst.add(edge);
            totalWeight += edge.getWeight();
            mstNodes.add(u);
            mstNodes.add(v);
            mstEdgeIds.add(edge.getId());
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Ajout : " + edge.getSource().getLabel() + " — " + edge.getTarget().getLabel(),
                    new HashSet<>(mstNodes),
                    Set.of(u, v),
                    new HashSet<>(mstNodes),
                    new HashSet<>(mstEdgeIds),
                    Map.of()
            ));
            if (mst.size() == graph.nodeCount() - 1) {
                break;
            }
        }

        if (mst.size() < graph.nodeCount() - 1) {
            result.setError("Le graphe n'est pas connexe — MST incomplet.");
            return result;
        }
        result.setSpanningTreeEdges(mst);
        result.setSpanningTreeWeight(totalWeight);
        result.log("Poids total du MST : " + Dijkstra.fmt(totalWeight));
        return result;
    }

    public static AlgorithmResult prim(Graph graph, int startId) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.SPANNING_TREE, "Prim");
        if (graph.nodeCount() == 0) { result.setError("Graphe vide."); return result; }
        if (graph.isDirected()) { result.setError("Prim s'applique à un graphe non orienté."); return result; }
        if (graph.getNode(startId).isEmpty()) { result.setError("Nœud source introuvable."); return result; }

        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Edge> queue = new PriorityQueue<>(Comparator.comparingDouble(Edge::getWeight));
        List<Edge> mst = new ArrayList<>();
        Set<Integer> mstEdgeIds = new HashSet<>();
        double totalWeight = 0.0;

        visited.add(startId);
        queue.addAll(graph.edgesOf(startId));
        result.log("=== Prim — départ depuis " + graph.getNode(startId).orElseThrow().getLabel() + " ===\n");

        while (!queue.isEmpty() && mst.size() < graph.nodeCount() - 1) {
            Edge edge = queue.poll();
            int u = edge.getSource().getId();
            int v = edge.getTarget().getId();
            int next = visited.contains(u) ? v : u;
            if (visited.contains(u) && visited.contains(v)) {
                continue;
            }
            visited.add(next);
            mst.add(edge);
            mstEdgeIds.add(edge.getId());
            totalWeight += edge.getWeight();
            for (Edge candidate : graph.edgesOf(next)) {
                int other = candidate.getSource().getId() == next ? candidate.getTarget().getId() : candidate.getSource().getId();
                if (!visited.contains(other)) {
                    queue.offer(candidate);
                }
            }
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Ajout MST via Prim : " + edge.getSource().getLabel() + " — " + edge.getTarget().getLabel(),
                    new HashSet<>(visited),
                    Set.of(next),
                    new HashSet<>(visited),
                    new HashSet<>(mstEdgeIds),
                    Map.of()
            ));
        }

        if (mst.size() < graph.nodeCount() - 1) {
            result.setError("Le graphe n'est pas connexe — MST incomplet.");
            return result;
        }
        result.setSpanningTreeEdges(mst);
        result.setSpanningTreeWeight(totalWeight);
        result.log("Poids total du MST : " + Dijkstra.fmt(totalWeight));
        return result;
    }

    private static int find(Map<Integer, Integer> parent, int node) {
        if (parent.get(node) != node) {
            parent.put(node, find(parent, parent.get(node)));
        }
        return parent.get(node);
    }

    private static void union(Map<Integer, Integer> parent, Map<Integer, Integer> rank, int a, int b) {
        if (rank.get(a) < rank.get(b)) {
            parent.put(a, b);
        } else if (rank.get(a) > rank.get(b)) {
            parent.put(b, a);
        } else {
            parent.put(b, a);
            rank.put(a, rank.get(a) + 1);
        }
    }
}
