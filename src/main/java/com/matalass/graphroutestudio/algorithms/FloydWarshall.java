package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FloydWarshall {
    private FloydWarshall() {
    }

    public static AlgorithmResult run(Graph graph) {
        AlgorithmResult result = new AlgorithmResult(AlgorithmResult.Type.ALL_PAIRS_SHORTEST, "Floyd-Warshall");
        List<Node> order = new ArrayList<>(graph.getNodes());
        int n = order.size();
        if (n == 0) {
            result.setError("Graphe vide.");
            return result;
        }

        double[][] dist = graph.adjacencyMatrix();
        int[][] next = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                next[i][j] = dist[i][j] < Double.MAX_VALUE && i != j ? j : -1;
            }
        }

        result.log("=== Floyd-Warshall — plus courts chemins tous couples ===\n");
        for (int k = 0; k < n; k++) {
            Node pivot = order.get(k);
            result.log("Pivot : " + pivot.getLabel());
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] == Double.MAX_VALUE || dist[k][j] == Double.MAX_VALUE) {
                        continue;
                    }
                    double alt = dist[i][k] + dist[k][j];
                    if (alt < dist[i][j]) {
                        dist[i][j] = alt;
                        next[i][j] = next[i][k];
                    }
                }
            }
            result.addStep(new AlgorithmResult.AnimationStep(
                    "Pivot " + pivot.getLabel(),
                    Set.of(),
                    Set.of(pivot.getId()),
                    Set.of(),
                    Set.of(),
                    java.util.Map.of()
            ));
        }

        for (int i = 0; i < n; i++) {
            if (dist[i][i] < 0) {
                result.setError("Cycle négatif détecté dans le graphe.");
                return result;
            }
        }

        result.setDistMatrix(dist);
        result.setNextMatrix(next);
        result.setNodeOrder(order);
        result.log("\nMatrice des distances calculée pour " + n + " nœuds.");
        return result;
    }
}
