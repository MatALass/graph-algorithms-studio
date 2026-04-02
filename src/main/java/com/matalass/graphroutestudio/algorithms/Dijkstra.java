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
import java.util.PriorityQueue;
import java.util.Set;

public final class Dijkstra {
    private Dijkstra() {
    }

    public static AlgorithmResult run(Graph graph, int sourceId, int targetId) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.SHORTEST_PATH, "Dijkstra");

        Optional<Node> srcOpt = graph.getNode(sourceId);
        Optional<Node> tgtOpt = graph.getNode(targetId);
        if (srcOpt.isEmpty()) { result.setError("Nœud source introuvable : " + sourceId); return result; }
        if (tgtOpt.isEmpty()) { result.setError("Nœud cible introuvable : " + targetId); return result; }

        for (Edge edge : graph.getEdges()) {
            if (edge.getWeight() < 0) {
                result.setError("Dijkstra ne supporte pas les poids négatifs. Utilisez Bellman-Ford.");
                return result;
            }
        }

        Node source = srcOpt.get();
        Node target = tgtOpt.get();
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, Integer> prev = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Integer> queue = new PriorityQueue<>((a, b) -> Double.compare(dist.get(a), dist.get(b)));

        for (Node node : graph.getNodes()) {
            dist.put(node.getId(), Double.MAX_VALUE);
        }
        dist.put(sourceId, 0.0);
        queue.offer(sourceId);

        result.log("=== Dijkstra : " + source.getLabel() + " → " + target.getLabel() + " ===");
        result.log("Initialisation : dist[" + source.getLabel() + "] = 0, toutes les autres = ∞\n");
        recordStep(result, visited, Set.of(sourceId), dist, "Initialisation : source = " + source.getLabel());

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }

            Node currentNode = graph.getNode(current).orElseThrow();
            result.log("Visite de " + currentNode.getLabel() + " (dist=" + fmt(dist.get(current)) + ")");
            recordStep(result, new HashSet<>(visited), Set.of(current), dist,
                    "Visite de " + currentNode.getLabel() + " — dist = " + fmt(dist.get(current)));

            if (current == targetId) {
                break;
            }

            for (Edge edge : graph.edgesOf(current)) {
                Node neighbor = edge.getTarget().getId() == current ? edge.getSource() : edge.getTarget();
                int next = neighbor.getId();
                if (visited.contains(next)) {
                    continue;
                }
                double alt = dist.get(current) + edge.getWeight();
                if (alt < dist.get(next)) {
                    dist.put(next, alt);
                    prev.put(next, current);
                    queue.offer(next);
                    result.log("  Relaxation : dist[" + neighbor.getLabel() + "] = " + fmt(alt)
                            + " via " + currentNode.getLabel());
                    recordStep(result, new HashSet<>(visited), Set.of(next), dist,
                            "Relax " + neighbor.getLabel() + " → " + fmt(alt));
                }
            }
        }

        if (dist.get(targetId) == Double.MAX_VALUE) {
            result.setError("Aucun chemin trouvé entre " + source.getLabel() + " et " + target.getLabel());
            return result;
        }

        List<Node> path = rebuildPath(graph, prev, sourceId, targetId);
        result.setPath(path);
        result.setDistances(dist);
        result.setPredecessors(prev);
        result.setPathWeight(dist.get(targetId));

        StringBuilder pathDescription = new StringBuilder("\nChemin optimal : ");
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                pathDescription.append(" → ");
            }
            pathDescription.append(path.get(i).getLabel());
        }
        pathDescription.append("\nCoût total : ").append(fmt(dist.get(targetId)));
        result.log(pathDescription.toString());

        Set<Integer> pathIds = new HashSet<>();
        for (Node node : path) {
            pathIds.add(node.getId());
        }
        result.addStep(new AlgorithmResult.AnimationStep(
                "Chemin optimal trouvé — coût = " + fmt(dist.get(targetId)),
                visited,
                pathIds,
                pathIds,
                buildPathEdgeIds(graph, path),
                new HashMap<>(dist)
        ));

        return result;
    }

    static List<Node> rebuildPath(Graph graph, Map<Integer, Integer> prev, int sourceId, int targetId) {
        List<Node> path = new ArrayList<>();
        int current = targetId;
        while (current != sourceId) {
            graph.getNode(current).ifPresent(node -> path.add(0, node));
            Integer predecessor = prev.get(current);
            if (predecessor == null) {
                return List.of();
            }
            current = predecessor;
        }
        graph.getNode(sourceId).ifPresent(node -> path.add(0, node));
        return path;
    }

    static Set<Integer> buildPathEdgeIds(Graph graph, List<Node> path) {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < path.size() - 1; i++) {
            int source = path.get(i).getId();
            int target = path.get(i + 1).getId();
            graph.findEdge(source, target)
                    .or(() -> graph.isDirected() ? Optional.empty() : graph.findEdge(target, source))
                    .ifPresent(edge -> ids.add(edge.getId()));
        }
        return ids;
    }

    static void recordStep(AlgorithmResult result, Set<Integer> visited, Set<Integer> active,
                           Map<Integer, Double> dist, String description) {
        result.addStep(new AlgorithmResult.AnimationStep(
                description,
                new HashSet<>(visited),
                new HashSet<>(active),
                Set.of(),
                Set.of(),
                new HashMap<>(dist)
        ));
    }

    static String fmt(double value) {
        if (value == Double.MAX_VALUE) return "∞";
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }
}
