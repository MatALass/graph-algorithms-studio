package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BellmanFord {
    private BellmanFord() {
    }

    public static AlgorithmResult run(Graph graph, int sourceId, int targetId) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.SHORTEST_PATH, "Bellman-Ford");
        Optional<Node> srcOpt = graph.getNode(sourceId);
        Optional<Node> tgtOpt = graph.getNode(targetId);
        if (srcOpt.isEmpty()) { result.setError("Nœud source introuvable : " + sourceId); return result; }
        if (tgtOpt.isEmpty()) { result.setError("Nœud cible introuvable : " + targetId); return result; }

        Node source = srcOpt.get();
        Node target = tgtOpt.get();
        int n = graph.nodeCount();
        List<Edge> edges = graph.getEdges();
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, Integer> prev = new HashMap<>();

        for (Node node : graph.getNodes()) {
            dist.put(node.getId(), Double.MAX_VALUE);
        }
        dist.put(sourceId, 0.0);

        result.log("=== Bellman-Ford : " + source.getLabel() + " → " + target.getLabel() + " ===");
        result.log("Initialisation : dist[source] = 0, autres = ∞\n");

        for (int iter = 0; iter < n - 1; iter++) {
            boolean updated = false;
            Set<Integer> relaxed = new HashSet<>();
            for (Edge edge : edges) {
                int u = edge.getSource().getId();
                int v = edge.getTarget().getId();
                if (dist.get(u) != Double.MAX_VALUE) {
                    double alt = dist.get(u) + edge.getWeight();
                    if (alt < dist.get(v)) {
                        dist.put(v, alt);
                        prev.put(v, u);
                        updated = true;
                        relaxed.add(v);
                        result.log("  Iter " + (iter + 1) + " : dist[" + edge.getTarget().getLabel() + "] = "
                                + Dijkstra.fmt(alt) + " via " + edge.getSource().getLabel());
                    }
                }
                if (!graph.isDirected() && dist.get(v) != Double.MAX_VALUE) {
                    double altRev = dist.get(v) + edge.getWeight();
                    if (altRev < dist.get(u)) {
                        dist.put(u, altRev);
                        prev.put(u, v);
                        updated = true;
                        relaxed.add(u);
                    }
                }
            }

            Set<Integer> visitedSoFar = new HashSet<>();
            for (Map.Entry<Integer, Double> entry : dist.entrySet()) {
                if (entry.getValue() < Double.MAX_VALUE) {
                    visitedSoFar.add(entry.getKey());
                }
            }
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Itération " + (iter + 1) + "/" + (n - 1),
                    visitedSoFar,
                    relaxed,
                    Set.of(),
                    Set.of(),
                    new HashMap<>(dist)
            ));

            if (!updated) {
                result.log("\nAucune modification : arrêt anticipé.");
                break;
            }
        }

        for (Edge edge : edges) {
            int u = edge.getSource().getId();
            int v = edge.getTarget().getId();
            if (dist.get(u) != Double.MAX_VALUE && dist.get(u) + edge.getWeight() < dist.get(v)) {
                result.setError("Cycle négatif détecté : plus court chemin non défini.");
                return result;
            }
        }

        if (dist.get(targetId) == Double.MAX_VALUE) {
            result.setError("Aucun chemin trouvé entre " + source.getLabel() + " et " + target.getLabel());
            return result;
        }

        List<Node> path = new ArrayList<>();
        int current = targetId;
        while (current != sourceId) {
            graph.getNode(current).ifPresent(node -> path.add(0, node));
            current = prev.get(current);
        }
        path.add(0, source);

        result.setPath(path);
        result.setDistances(dist);
        result.setPredecessors(prev);
        result.setPathWeight(dist.get(targetId));

        Set<Integer> pathIds = new HashSet<>();
        path.forEach(node -> pathIds.add(node.getId()));
        result.addStep(new AlgorithmResult.AnimationStep(
                "Chemin optimal Bellman-Ford — coût = " + Dijkstra.fmt(dist.get(targetId)),
                new HashSet<>(dist.keySet()),
                pathIds,
                pathIds,
                Dijkstra.buildPathEdgeIds(graph, path),
                new HashMap<>(dist)
        ));
        result.log("\nChemin optimal : coût total = " + Dijkstra.fmt(dist.get(targetId)));
        return result;
    }
}
