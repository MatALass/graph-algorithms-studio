package com.matalass.graphroutestudio.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Graph {
    private final Map<Integer, Node> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private boolean directed;
    private boolean weighted;
    private String name;
    private int nextNodeId = 0;

    public Graph(boolean directed, boolean weighted, String name) {
        this.directed = directed;
        this.weighted = weighted;
        this.name = name;
    }

    public Graph() {
        this(true, true, "Nouveau graphe");
    }

    public Node addNode(double x, double y) {
        Node node = new Node(nextNodeId++, x, y);
        nodes.put(node.getId(), node);
        return node;
    }

    public Node addNode(int id, String label, double x, double y) {
        if (id >= nextNodeId) {
            nextNodeId = id + 1;
        }
        Node node = new Node(id, label, x, y);
        nodes.put(node.getId(), node);
        return node;
    }

    public boolean removeNode(int id) {
        Node removed = nodes.remove(id);
        if (removed == null) {
            return false;
        }
        edges.removeIf(edge -> edge.getSource().equals(removed) || edge.getTarget().equals(removed));
        return true;
    }

    public Optional<Node> getNode(int id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public List<Node> nodeList() {
        return new ArrayList<>(nodes.values());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public Edge addEdge(int sourceId, int targetId, double weight) {
        Node source = nodes.get(sourceId);
        Node target = nodes.get(targetId);
        if (source == null || target == null) {
            throw new IllegalArgumentException("Nœud inexistant");
        }
        Edge edge = new Edge(source, target, weight);
        edges.add(edge);
        return edge;
    }

    public Edge addEdge(int sourceId, int targetId) {
        return addEdge(sourceId, targetId, 1.0);
    }

    public boolean removeEdge(Edge edge) {
        return edges.remove(edge);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Optional<Edge> findEdge(int sourceId, int targetId) {
        return edges.stream()
                .filter(edge -> edge.getSource().getId() == sourceId && edge.getTarget().getId() == targetId)
                .findFirst();
    }

    public int edgeCount() {
        return edges.size();
    }

    public List<Edge> edgesOf(int nodeId) {
        Node node = nodes.get(nodeId);
        if (node == null) {
            return List.of();
        }
        List<Edge> result = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getSource().equals(node)) {
                result.add(edge);
            } else if (!directed && edge.getTarget().equals(node)) {
                result.add(edge);
            }
        }
        return result;
    }

    public List<Node> neighbors(int nodeId) {
        Node node = nodes.get(nodeId);
        if (node == null) {
            return List.of();
        }
        List<Node> result = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getSource().equals(node)) {
                result.add(edge.getTarget());
            } else if (!directed && edge.getTarget().equals(node)) {
                result.add(edge.getSource());
            }
        }
        return result;
    }

    public double[][] adjacencyMatrix() {
        List<Node> nodeList = nodeList();
        int n = nodeList.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = i == j ? 0.0 : Double.MAX_VALUE;
            }
        }
        Map<Integer, Integer> indexById = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indexById.put(nodeList.get(i).getId(), i);
        }
        for (Edge edge : edges) {
            int i = indexById.get(edge.getSource().getId());
            int j = indexById.get(edge.getTarget().getId());
            matrix[i][j] = edge.getWeight();
            if (!directed) {
                matrix[j][i] = edge.getWeight();
            }
        }
        return matrix;
    }

    public void clearVisualState() {
        for (Node node : nodes.values()) {
            node.setHighlighted(false);
            node.setOnPath(false);
            node.setColor("#4A90D9");
        }
        for (Edge edge : edges) {
            edge.setHighlighted(false);
            edge.setOnPath(false);
            edge.setFlow(0);
        }
    }

    public boolean hasNegativeWeights() {
        return edges.stream().anyMatch(edge -> edge.getWeight() < 0);
    }

    public double totalWeight() {
        return edges.stream().mapToDouble(Edge::getWeight).sum();
    }

    public boolean isDirected() { return directed; }
    public void setDirected(boolean directed) { this.directed = directed; }
    public boolean isWeighted() { return weighted; }
    public void setWeighted(boolean weighted) { this.weighted = weighted; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void clear() {
        nodes.clear();
        edges.clear();
        nextNodeId = 0;
    }

    @Override
    public String toString() {
        return "Graph{name='" + name + "', nodes=" + nodeCount() + ", edges=" + edgeCount()
                + ", directed=" + directed + ", weighted=" + weighted + "}";
    }
}
