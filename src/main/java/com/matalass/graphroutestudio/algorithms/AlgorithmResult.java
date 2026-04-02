package com.matalass.graphroutestudio.algorithms;

import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlgorithmResult {
    public enum Type {
        SHORTEST_PATH,
        ALL_PAIRS_SHORTEST,
        SPANNING_TREE,
        CYCLE_DETECTION,
        MAX_FLOW
    }

    private final Type type;
    private final String algorithmName;

    private List<Node> path = new ArrayList<>();
    private Map<Integer, Double> distances = new HashMap<>();
    private Map<Integer, Integer> predecessors = new HashMap<>();
    private double pathWeight = 0;

    private double[][] distMatrix;
    private int[][] nextMatrix;
    private List<Node> nodeOrder;

    private List<Edge> spanningTreeEdges = new ArrayList<>();
    private double spanningTreeWeight = 0;

    private boolean hasCycle = false;
    private List<Node> cycleNodes = new ArrayList<>();

    private int maxFlow = 0;
    private Map<Edge, Integer> flowMap = new HashMap<>();

    private final StringBuilder log = new StringBuilder();
    private final List<AnimationStep> steps = new ArrayList<>();

    private boolean success = true;
    private String errorMessage = "";

    public record AnimationStep(
            String description,
            Set<Integer> visitedNodes,
            Set<Integer> activeNodes,
            Set<Integer> pathNodes,
            Set<Integer> pathEdgeIds,
            Map<Integer, Double> currentDistances
    ) {
    }

    public AlgorithmResult(Type type, String algorithmName) {
        this.type = type;
        this.algorithmName = algorithmName;
    }

    public void addStep(AnimationStep step) { steps.add(step); }
    public List<AnimationStep> getSteps() { return Collections.unmodifiableList(steps); }
    public void log(String line) { log.append(line).append("\n"); }
    public String getLog() { return log.toString(); }

    public Type getType() { return type; }
    public String getAlgorithmName() { return algorithmName; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public void setError(String message) { success = false; errorMessage = message; log("ERREUR : " + message); }

    public List<Node> getPath() { return path; }
    public void setPath(List<Node> path) { this.path = path; }
    public Map<Integer, Double> getDistances() { return distances; }
    public void setDistances(Map<Integer, Double> distances) { this.distances = distances; }
    public Map<Integer, Integer> getPredecessors() { return predecessors; }
    public void setPredecessors(Map<Integer, Integer> predecessors) { this.predecessors = predecessors; }
    public double getPathWeight() { return pathWeight; }
    public void setPathWeight(double pathWeight) { this.pathWeight = pathWeight; }
    public double[][] getDistMatrix() { return distMatrix; }
    public void setDistMatrix(double[][] distMatrix) { this.distMatrix = distMatrix; }
    public int[][] getNextMatrix() { return nextMatrix; }
    public void setNextMatrix(int[][] nextMatrix) { this.nextMatrix = nextMatrix; }
    public List<Node> getNodeOrder() { return nodeOrder; }
    public void setNodeOrder(List<Node> nodeOrder) { this.nodeOrder = nodeOrder; }
    public List<Edge> getSpanningTreeEdges() { return spanningTreeEdges; }
    public void setSpanningTreeEdges(List<Edge> spanningTreeEdges) { this.spanningTreeEdges = spanningTreeEdges; }
    public double getSpanningTreeWeight() { return spanningTreeWeight; }
    public void setSpanningTreeWeight(double spanningTreeWeight) { this.spanningTreeWeight = spanningTreeWeight; }
    public boolean hasCycle() { return hasCycle; }
    public void setHasCycle(boolean hasCycle) { this.hasCycle = hasCycle; }
    public List<Node> getCycleNodes() { return cycleNodes; }
    public void setCycleNodes(List<Node> cycleNodes) { this.cycleNodes = cycleNodes; }
    public int getMaxFlow() { return maxFlow; }
    public void setMaxFlow(int maxFlow) { this.maxFlow = maxFlow; }
    public Map<Edge, Integer> getFlowMap() { return flowMap; }
    public void setFlowMap(Map<Edge, Integer> flowMap) { this.flowMap = flowMap; }
}
