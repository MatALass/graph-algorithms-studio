package com.matalass.graphroutestudio.engine;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Node {
    private final int id;
    private final StringProperty label;
    private final DoubleProperty x;
    private final DoubleProperty y;

    private boolean highlighted = false;
    private boolean onPath = false;
    private String color = "#4A90D9";

    public Node(int id, String label, double x, double y) {
        this.id = id;
        this.label = new SimpleStringProperty(label);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
    }

    public Node(int id, double x, double y) {
        this(id, String.valueOf(id), x, y);
    }

    public int getId() { return id; }
    public String getLabel() { return label.get(); }
    public void setLabel(String value) { label.set(value); }
    public StringProperty labelProperty() { return label; }

    public double getX() { return x.get(); }
    public void setX(double value) { x.set(value); }
    public DoubleProperty xProperty() { return x; }

    public double getY() { return y.get(); }
    public void setY(double value) { y.set(value); }
    public DoubleProperty yProperty() { return y; }

    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    public boolean isOnPath() { return onPath; }
    public void setOnPath(boolean onPath) { this.onPath = onPath; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() {
        return "Node{id=" + id + ", label='" + getLabel() + "', x=" + getX() + ", y=" + getY() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
