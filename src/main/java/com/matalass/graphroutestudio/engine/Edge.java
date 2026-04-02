package com.matalass.graphroutestudio.engine;

public class Edge {
    private final int id;
    private final Node source;
    private final Node target;
    private double weight;
    private int capacity;
    private int flow;

    private boolean highlighted = false;
    private boolean onPath = false;

    private static int counter = 0;

    public Edge(Node source, Node target, double weight) {
        this.id = counter++;
        this.source = source;
        this.target = target;
        this.weight = weight;
        this.capacity = Math.max(0, (int) Math.round(weight));
        this.flow = 0;
    }

    public Edge(Node source, Node target) {
        this(source, target, 1.0);
    }

    public int getId() { return id; }
    public Node getSource() { return source; }
    public Node getTarget() { return target; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) {
        this.weight = weight;
        this.capacity = Math.max(0, (int) Math.round(weight));
    }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getFlow() { return flow; }
    public void setFlow(int flow) { this.flow = flow; }
    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }
    public boolean isOnPath() { return onPath; }
    public void setOnPath(boolean onPath) { this.onPath = onPath; }

    public boolean connects(Node a, Node b) {
        return (source.equals(a) && target.equals(b)) || (source.equals(b) && target.equals(a));
    }

    @Override
    public String toString() {
        return "Edge{" + source.getId() + " -> " + target.getId() + ", w=" + weight + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
