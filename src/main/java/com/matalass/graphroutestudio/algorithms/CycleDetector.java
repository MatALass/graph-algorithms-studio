package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CycleDetector {
    private CycleDetector() {
    }

    private enum Color { WHITE, GRAY, BLACK }

    public static AlgorithmResult run(Graph graph) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.CYCLE_DETECTION, "Détection de cycles");
        result.log("=== Détection de cycles — DFS ===\n");
        result.log("Mode : " + (graph.isDirected() ? "graphe orienté" : "graphe non orienté") + "\n");

        Map<Integer, Color> color = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        List<Integer> cycleNodes = new ArrayList<>();
        boolean[] foundCycle = {false};
        for (Node node : graph.getNodes()) {
            color.put(node.getId(), Color.WHITE);
            parent.put(node.getId(), -1);
        }

        Set<Integer> visited = new HashSet<>();
        for (Node start : graph.getNodes()) {
            if (color.get(start.getId()) == Color.WHITE) {
                dfs(graph, start.getId(), color, parent, visited, cycleNodes, foundCycle, result);
                if (foundCycle[0]) {
                    break;
                }
            }
        }

        if (foundCycle[0]) {
            result.setHasCycle(true);
            List<Node> cycleNodeObjs = new ArrayList<>();
            for (int id : cycleNodes) {
                graph.getNode(id).ifPresent(cycleNodeObjs::add);
            }
            result.setCycleNodes(cycleNodeObjs);
            result.log("\nCYCLE DÉTECTÉ !");
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Cycle détecté !",
                    new HashSet<>(visited),
                    new HashSet<>(cycleNodes),
                    new HashSet<>(cycleNodes),
                    Set.of(),
                    Map.of()
            ));
        } else {
            result.setHasCycle(false);
            result.log("\nAucun cycle détecté.");
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Aucun cycle",
                    new HashSet<>(visited),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Map.of()
            ));
        }
        return result;
    }

    private static void dfs(Graph graph, int current, Map<Integer, Color> color, Map<Integer, Integer> parent,
                            Set<Integer> visited, List<Integer> cycleNodes, boolean[] foundCycle,
                            AlgorithmResult result) {
        if (foundCycle[0]) {
            return;
        }
        color.put(current, Color.GRAY);
        visited.add(current);
        result.addStep(new AlgorithmResult.AnimationStep(
                "Exploration de " + graph.getNode(current).orElseThrow().getLabel(),
                new HashSet<>(visited),
                Set.of(current),
                Set.of(),
                Set.of(),
                Map.of()
        ));

        for (Node neighbor : graph.neighbors(current)) {
            int next = neighbor.getId();
            if (foundCycle[0]) {
                return;
            }
            Color nextColor = color.get(next);
            if (nextColor == Color.GRAY) {
                if (graph.isDirected() || parent.get(current) != next) {
                    foundCycle[0] = true;
                    cycleNodes.add(next);
                    int cursor = current;
                    while (cursor != next) {
                        cycleNodes.add(0, cursor);
                        cursor = parent.getOrDefault(cursor, next);
                    }
                    cycleNodes.add(0, next);
                    return;
                }
            } else if (nextColor == Color.WHITE) {
                parent.put(next, current);
                dfs(graph, next, color, parent, visited, cycleNodes, foundCycle, result);
            }
        }

        color.put(current, Color.BLACK);
    }
}
