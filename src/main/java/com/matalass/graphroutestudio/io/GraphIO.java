package com.matalass.graphroutestudio.io;

import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GraphIO {
    private GraphIO() {
    }

    public static void saveJson(Graph graph, File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": ").append(jsonStr(graph.getName())).append(",\n");
        sb.append("  \"directed\": ").append(graph.isDirected()).append(",\n");
        sb.append("  \"weighted\": ").append(graph.isWeighted()).append(",\n");
        sb.append("  \"nodes\": [\n");
        List<Node> nodes = new ArrayList<>(graph.getNodes());
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            sb.append("    {\"id\": ").append(node.getId())
                    .append(", \"label\": ").append(jsonStr(node.getLabel()))
                    .append(", \"x\": ").append(String.format("%.1f", node.getX()))
                    .append(", \"y\": ").append(String.format("%.1f", node.getY()))
                    .append("}");
            if (i < nodes.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"edges\": [\n");
        List<Edge> edges = graph.getEdges();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            sb.append("    {\"source\": ").append(edge.getSource().getId())
                    .append(", \"target\": ").append(edge.getTarget().getId())
                    .append(", \"weight\": ").append(edge.getWeight())
                    .append("}");
            if (i < edges.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        Files.writeString(file.toPath(), sb.toString());
    }

    public static Graph loadJson(File file) throws IOException {
        String content = Files.readString(file.toPath());
        boolean directed = parseBoolean(content, "directed", true);
        boolean weighted = parseBoolean(content, "weighted", true);
        String name = parseString(content, "name", file.getName());
        Graph graph = new Graph(directed, weighted, name);

        String nodesSection = extractSection(content, "nodes");
        for (String nodeJson : splitObjects(nodesSection)) {
            int id = parseInt(nodeJson, "id", -1);
            String label = parseString(nodeJson, "label", String.valueOf(id));
            double x = parseDouble(nodeJson, "x", 100);
            double y = parseDouble(nodeJson, "y", 100);
            if (id >= 0) {
                graph.addNode(id, label, x, y);
            }
        }

        String edgesSection = extractSection(content, "edges");
        for (String edgeJson : splitObjects(edgesSection)) {
            int src = parseInt(edgeJson, "source", -1);
            int tgt = parseInt(edgeJson, "target", -1);
            double w = parseDouble(edgeJson, "weight", 1.0);
            if (src >= 0 && tgt >= 0) {
                graph.addEdge(src, tgt, w);
            }
        }
        return graph;
    }

    public static Graph loadConstraintTable(File file, double canvasW, double canvasH) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        Graph graph = new Graph(true, true, file.getName());
        int n = lines.size();
        double cx = canvasW / 2;
        double cy = canvasH / 2;
        double r = Math.min(cx, cy) * 0.75;
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            graph.addNode(i + 1, String.valueOf(i + 1), cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2) continue;
            try {
                int task = Integer.parseInt(parts[0]);
                double weight = Double.parseDouble(parts[1]);
                for (int i = 2; i < parts.length; i++) {
                    int predecessor = Integer.parseInt(parts[i]);
                    graph.addEdge(predecessor, task, weight);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return graph;
    }

    public static Graph generateRandom(int nodeCount, double edgeProbability,
                                       boolean directed, boolean weighted,
                                       double canvasW, double canvasH) {
        Graph graph = new Graph(directed, weighted, "Aléatoire " + nodeCount + " nœuds");
        Random random = new Random();
        double cx = canvasW / 2;
        double cy = canvasH / 2;
        double r = Math.min(cx, cy) * 0.78;
        for (int i = 0; i < nodeCount; i++) {
            double angle = 2 * Math.PI * i / nodeCount - Math.PI / 2;
            String label = String.valueOf((char) ('A' + (i % 26))) + (i >= 26 ? i / 26 : "");
            graph.addNode(i, label, cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }
        for (int i = 0; i < nodeCount; i++) {
            for (int j = directed ? 0 : i + 1; j < nodeCount; j++) {
                if (i == j) continue;
                if (random.nextDouble() < edgeProbability) {
                    double weight = weighted ? Math.round((1 + random.nextDouble() * 9) * 10.0) / 10.0 : 1.0;
                    graph.addEdge(i, j, weight);
                }
            }
        }
        return graph;
    }

    private static String jsonStr(String value) { return "\"" + value.replace("\"", "\\\"") + "\""; }
    private static String parseString(String json, String key, String def) {
        var m = java.util.regex.Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*?)\\\"").matcher(json);
        return m.find() ? m.group(1) : def;
    }
    private static boolean parseBoolean(String json, String key, boolean def) {
        var m = java.util.regex.Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : def;
    }
    private static int parseInt(String json, String key, int def) {
        var m = java.util.regex.Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }
    private static double parseDouble(String json, String key, double def) {
        var m = java.util.regex.Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?[\\d.]+)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : def;
    }
    private static String extractSection(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) return "[]";
        int bracket = json.indexOf('[', start);
        if (bracket < 0) return "[]";
        int depth = 0;
        int end = bracket;
        for (int i = bracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        return json.substring(bracket, end + 1);
    }
    private static List<String> splitObjects(String arrayJson) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) result.add(arrayJson.substring(start, i + 1));
            }
        }
        return result;
    }
}
