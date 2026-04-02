package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public final class AStar {
    private AStar() {
    }

    public static AlgorithmResult run(Graph graph, int sourceId, int targetId) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.SHORTEST_PATH, "A*");
        Optional<Node> srcOpt = graph.getNode(sourceId);
        Optional<Node> tgtOpt = graph.getNode(targetId);
        if (srcOpt.isEmpty()) { result.setError("Nœud source introuvable : " + sourceId); return result; }
        if (tgtOpt.isEmpty()) { result.setError("Nœud cible introuvable : " + targetId); return result; }

        for (Edge edge : graph.getEdges()) {
            if (edge.getWeight() < 0) {
                result.setError("A* suppose des poids non négatifs.");
                return result;
            }
        }

        Node source = srcOpt.get();
        Node target = tgtOpt.get();
        Map<Integer, Double> gScore = new HashMap<>();
        Map<Integer, Double> fScore = new HashMap<>();
        Map<Integer, Integer> prev = new HashMap<>();
        Set<Integer> closed = new HashSet<>();
        PriorityQueue<Integer> open = new PriorityQueue<>((a, b) -> Double.compare(fScore.get(a), fScore.get(b)));

        for (Node node : graph.getNodes()) {
            gScore.put(node.getId(), Double.MAX_VALUE);
            fScore.put(node.getId(), Double.MAX_VALUE);
        }
        gScore.put(sourceId, 0.0);
        fScore.put(sourceId, heuristic(source, target));
        open.offer(sourceId);

        result.log("=== A* : " + source.getLabel() + " → " + target.getLabel() + " ===");
        result.log("Heuristique : distance euclidienne entre les positions des nœuds.\n");
        Dijkstra.recordStep(result, Set.of(), Set.of(sourceId), gScore, "Initialisation A*");

        while (!open.isEmpty()) {
            int current = open.poll();
            if (!closed.add(current)) {
                continue;
            }
            Node currentNode = graph.getNode(current).orElseThrow();
            result.log("Visite de " + currentNode.getLabel() + " (g=" + Dijkstra.fmt(gScore.get(current))
                    + ", f=" + Dijkstra.fmt(fScore.get(current)) + ")");
            Dijkstra.recordStep(result, new HashSet<>(closed), Set.of(current), gScore,
                    "Exploration A* de " + currentNode.getLabel());

            if (current == targetId) {
                break;
            }

            for (Edge edge : graph.edgesOf(current)) {
                Node neighbor = edge.getTarget().getId() == current ? edge.getSource() : edge.getTarget();
                int next = neighbor.getId();
                if (closed.contains(next)) {
                    continue;
                }
                double tentative = gScore.get(current) + edge.getWeight();
                if (tentative < gScore.get(next)) {
                    prev.put(next, current);
                    gScore.put(next, tentative);
                    fScore.put(next, tentative + heuristic(neighbor, target));
                    open.offer(next);
                    result.log("  Mise à jour " + neighbor.getLabel() + " : g=" + Dijkstra.fmt(tentative)
                            + ", f=" + Dijkstra.fmt(fScore.get(next)));
                    Dijkstra.recordStep(result, new HashSet<>(closed), Set.of(next), gScore,
                            "Frontière A* : " + neighbor.getLabel());
                }
            }
        }

        if (gScore.get(targetId) == Double.MAX_VALUE) {
            result.setError("Aucun chemin trouvé entre " + source.getLabel() + " et " + target.getLabel());
            return result;
        }

        var path = Dijkstra.rebuildPath(graph, prev, sourceId, targetId);
        result.setPath(path);
        result.setDistances(gScore);
        result.setPredecessors(prev);
        result.setPathWeight(gScore.get(targetId));

        Set<Integer> pathIds = new HashSet<>();
        path.forEach(node -> pathIds.add(node.getId()));
        result.addStep(new AlgorithmResult.AnimationStep(
                "Chemin A* trouvé — coût = " + Dijkstra.fmt(gScore.get(targetId)),
                new HashSet<>(closed),
                pathIds,
                pathIds,
                Dijkstra.buildPathEdgeIds(graph, path),
                new HashMap<>(gScore)
        ));
        result.log("\nChemin trouvé avec A* — coût total : " + Dijkstra.fmt(gScore.get(targetId)));
        return result;
    }

    private static double heuristic(Node a, Node b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.hypot(dx, dy);
    }
}
