package com.matalass.graphroutestudio.presets;

import com.matalass.graphroutestudio.engine.Graph;

public final class GraphPresets {
    private GraphPresets() {
    }

    public static Graph demoWeightedNetwork(double width, double height) {
        Graph graph = new Graph(false, true, "Demo réseau pondéré");
        double cx = width / 2;
        double cy = height / 2;
        double r = Math.min(width, height) * 0.28;
        String[] labels = {"A", "B", "C", "D", "E", "F", "G"};
        for (int i = 0; i < labels.length; i++) {
            double angle = 2 * Math.PI * i / labels.length - Math.PI / 2;
            graph.addNode(i, labels[i], cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }
        int[][] edges = {{0,1,4},{0,2,2},{1,2,5},{1,3,10},{2,3,3},{2,4,8},{3,5,7},{4,5,2},{4,6,5},{5,6,3}};
        for (int[] edge : edges) graph.addEdge(edge[0], edge[1], edge[2]);
        return graph;
    }

    public static Graph transportNetwork(double width, double height) {
        Graph graph = new Graph(false, true, "TransitPath – preset transport");
        graph.addNode(0, "Gare", width * 0.18, height * 0.30);
        graph.addNode(1, "Centre", width * 0.38, height * 0.20);
        graph.addNode(2, "Université", width * 0.62, height * 0.20);
        graph.addNode(3, "Hôpital", width * 0.80, height * 0.35);
        graph.addNode(4, "Stade", width * 0.20, height * 0.65);
        graph.addNode(5, "Mairie", width * 0.42, height * 0.52);
        graph.addNode(6, "Zone Tech", width * 0.66, height * 0.58);
        graph.addNode(7, "Aéroport", width * 0.84, height * 0.70);

        int[][] edges = {
                {0,1,4},{1,2,3},{2,3,5},{0,4,6},{4,5,4},{5,6,3},{6,7,4},
                {1,5,5},{2,6,4},{3,7,6},{5,2,2},{6,3,3},{4,1,7}
        };
        for (int[] edge : edges) graph.addEdge(edge[0], edge[1], edge[2]);
        return graph;
    }
}
