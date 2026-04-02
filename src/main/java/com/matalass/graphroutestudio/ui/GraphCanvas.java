package com.matalass.graphroutestudio.ui;

import com.matalass.graphroutestudio.algorithms.AlgorithmResult;
import com.matalass.graphroutestudio.engine.Edge;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GraphCanvas extends Canvas {
    private static final double NODE_RADIUS = 24;
    private static final double ARROW_SIZE = 10;
    private static final Color BG = Color.web("#1A1D27");
    private static final Color NODE_DEFAULT = Color.web("#4A90D9");
    private static final Color NODE_VISITED = Color.web("#6BCB77");
    private static final Color NODE_ACTIVE = Color.web("#FFD166");
    private static final Color NODE_PATH = Color.web("#EF476F");
    private static final Color EDGE_DEFAULT = Color.web("#4A5568");
    private static final Color EDGE_PATH = Color.web("#EF476F");
    private static final Color EDGE_MST = Color.web("#6BCB77");
    private static final Color LABEL_COLOR = Color.web("#A0AEC0");

    public enum EditMode { VIEW, EDIT, ADD_EDGE }

    private Graph graph;
    private EditMode mode = EditMode.EDIT;
    private AlgorithmResult currentResult;
    private AlgorithmResult.AnimationStep animationStep;

    private Node draggingNode;
    private Node edgeSourceNode;
    private double dragOffsetX;
    private double dragOffsetY;

    private Integer selectedSourceId;
    private Integer selectedTargetId;
    private double newEdgeWeight = 1.0;

    private Consumer<String> onStatusChange;
    private Runnable onGraphChanged;
    private Consumer<Node> onNodeRightClicked;

    public GraphCanvas(double width, double height) {
        super(width, height);
        setupInteractions();
    }

    private void setupInteractions() {
        setOnMousePressed(event -> {
            if (graph == null) return;
            Node hit = nodeAt(event.getX(), event.getY());

            if (event.getButton() == MouseButton.PRIMARY) {
                if (hit != null) {
                    if (mode == EditMode.ADD_EDGE) {
                        if (edgeSourceNode == null) {
                            edgeSourceNode = hit;
                            status("Arc : source = " + hit.getLabel() + " — cliquez sur la cible");
                        } else if (!edgeSourceNode.equals(hit)) {
                            graph.addEdge(edgeSourceNode.getId(), hit.getId(), newEdgeWeight);
                            edgeSourceNode = null;
                            status("Arc ajouté");
                            fireGraphChanged();
                        }
                    } else if (mode == EditMode.EDIT) {
                        draggingNode = hit;
                        dragOffsetX = event.getX() - hit.getX();
                        dragOffsetY = event.getY() - hit.getY();
                    }
                } else if (mode == EditMode.EDIT) {
                    Node node = graph.addNode(event.getX(), event.getY());
                    status("Nœud " + node.getLabel() + " créé");
                    fireGraphChanged();
                }
            } else if (event.getButton() == MouseButton.SECONDARY && hit != null) {
                if (onNodeRightClicked != null) {
                    onNodeRightClicked.accept(hit);
                }
            }
            redraw();
        });

        setOnMouseDragged(event -> {
            if (draggingNode != null) {
                double nx = Math.max(NODE_RADIUS, Math.min(getWidth() - NODE_RADIUS, event.getX() - dragOffsetX));
                double ny = Math.max(NODE_RADIUS, Math.min(getHeight() - NODE_RADIUS, event.getY() - dragOffsetY));
                draggingNode.setX(nx);
                draggingNode.setY(ny);
                redraw();
            }
        });

        setOnMouseReleased(event -> {
            if (draggingNode != null) {
                draggingNode = null;
                fireGraphChanged();
            }
        });

        setOnMouseClicked(event -> {
            if (graph == null || event.getClickCount() != 2 || mode != EditMode.EDIT) return;
            Node hit = nodeAt(event.getX(), event.getY());
            if (hit != null) {
                graph.removeNode(hit.getId());
                if (selectedSourceId != null && selectedSourceId == hit.getId()) selectedSourceId = null;
                if (selectedTargetId != null && selectedTargetId == hit.getId()) selectedTargetId = null;
                status("Nœud supprimé : " + hit.getLabel());
                fireGraphChanged();
            }
            redraw();
        });
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        clearSelections();
        currentResult = null;
        animationStep = null;
        redraw();
    }

    public void showResult(AlgorithmResult result) {
        this.currentResult = result;
        this.animationStep = null;
        redraw();
    }

    public void showAnimationStep(AlgorithmResult.AnimationStep step) {
        this.animationStep = step;
        redraw();
    }

    public void clearAnimationStep() {
        this.animationStep = null;
        redraw();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(BG);
        gc.fillRect(0, 0, getWidth(), getHeight());
        if (graph == null) {
            return;
        }
        for (Edge edge : graph.getEdges()) {
            drawEdge(gc, edge);
        }
        for (Node node : graph.getNodes()) {
            drawNode(gc, node);
        }
        drawLegend(gc);
    }

    private void drawEdge(GraphicsContext gc, Edge edge) {
        Node source = edge.getSource();
        Node target = edge.getTarget();
        double x1 = source.getX();
        double y1 = source.getY();
        double x2 = target.getX();
        double y2 = target.getY();
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double sx = x1 + NODE_RADIUS * Math.cos(angle);
        double sy = y1 + NODE_RADIUS * Math.sin(angle);
        double tx = x2 - NODE_RADIUS * Math.cos(angle);
        double ty = y2 - NODE_RADIUS * Math.sin(angle);

        gc.setStroke(resolveEdgeColor(edge));
        gc.setLineWidth(isPathEdge(edge) ? 4 : 2.5);
        gc.strokeLine(sx, sy, tx, ty);
        if (graph.isDirected()) {
            drawArrow(gc, tx, ty, angle);
        }

        gc.setFill(LABEL_COLOR);
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        gc.fillText(formatEdgeLabel(edge), (sx + tx) / 2 + 6, (sy + ty) / 2 - 6);
    }

    private void drawArrow(GraphicsContext gc, double x, double y, double angle) {
        double xA = x - ARROW_SIZE * Math.cos(angle - Math.PI / 6);
        double yA = y - ARROW_SIZE * Math.sin(angle - Math.PI / 6);
        double xB = x - ARROW_SIZE * Math.cos(angle + Math.PI / 6);
        double yB = y - ARROW_SIZE * Math.sin(angle + Math.PI / 6);
        gc.strokeLine(x, y, xA, yA);
        gc.strokeLine(x, y, xB, yB);
    }

    private void drawNode(GraphicsContext gc, Node node) {
        double x = node.getX();
        double y = node.getY();
        Color fill = resolveNodeColor(node);
        gc.setFill(fill);
        gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(isSelected(node.getId()) ? 3 : 1.5);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(node.getLabel(), x, y + 4);
    }

    private void drawLegend(GraphicsContext gc) {
        gc.setFill(Color.web("#0D0F19", 0.85));
        gc.fillRoundRect(12, 12, 255, 78, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.fillText("Mode: " + mode + " | clic droit = source/cible", 22, 34);
        gc.setFont(Font.font("System", 11));
        gc.fillText("Double-clic : supprimer nœud | Drag : déplacer", 22, 54);
        gc.fillText("Add edge : cliquer source puis cible", 22, 72);
    }

    private Color resolveNodeColor(Node node) {
        int id = node.getId();
        if (animationStep != null) {
            if (animationStep.pathNodes().contains(id)) return NODE_PATH;
            if (animationStep.activeNodes().contains(id)) return NODE_ACTIVE;
            if (animationStep.visitedNodes().contains(id)) return NODE_VISITED;
        }
        if (currentResult != null && currentResult.getCycleNodes().stream().anyMatch(n -> n.getId() == id)) {
            return Color.web("#FF6B6B");
        }
        if (currentResult != null && currentResult.getPath().stream().anyMatch(n -> n.getId() == id)) {
            return NODE_PATH;
        }
        return NODE_DEFAULT;
    }

    private Color resolveEdgeColor(Edge edge) {
        if (animationStep != null && animationStep.pathEdgeIds().contains(edge.getId())) return EDGE_PATH;
        if (currentResult != null && currentResult.getSpanningTreeEdges().contains(edge)) return EDGE_MST;
        if (currentResult != null && DijkstraEdgeMatcher.isPathEdge(currentResult, edge)) return EDGE_PATH;
        return EDGE_DEFAULT;
    }

    private boolean isPathEdge(Edge edge) {
        return (animationStep != null && animationStep.pathEdgeIds().contains(edge.getId()))
                || (currentResult != null && DijkstraEdgeMatcher.isPathEdge(currentResult, edge));
    }

    private String formatEdgeLabel(Edge edge) {
        if (currentResult != null && currentResult.getFlowMap().containsKey(edge) && currentResult.getFlowMap().get(edge) > 0) {
            return edge.getWeight() + " | f=" + currentResult.getFlowMap().get(edge);
        }
        return graph != null && graph.isWeighted() ? String.valueOf(edge.getWeight()) : "";
    }

    private Node nodeAt(double x, double y) {
        if (graph == null) return null;
        for (Node node : graph.getNodes()) {
            double dx = node.getX() - x;
            double dy = node.getY() - y;
            if (Math.hypot(dx, dy) <= NODE_RADIUS) {
                return node;
            }
        }
        return null;
    }

    private boolean isSelected(int nodeId) {
        return (selectedSourceId != null && selectedSourceId == nodeId)
                || (selectedTargetId != null && selectedTargetId == nodeId);
    }

    public void clearSelections() {
        selectedSourceId = null;
        selectedTargetId = null;
        edgeSourceNode = null;
    }

    public Graph getGraph() { return graph; }
    public EditMode getMode() { return mode; }
    public void setMode(EditMode mode) { this.mode = mode; status("Mode : " + mode); redraw(); }
    public Integer getSelectedSourceId() { return selectedSourceId; }
    public Integer getSelectedTargetId() { return selectedTargetId; }
    public void setSelectedSource(Integer selectedSourceId) { this.selectedSourceId = selectedSourceId; redraw(); }
    public void setSelectedTarget(Integer selectedTargetId) { this.selectedTargetId = selectedTargetId; redraw(); }
    public void setNewEdgeWeight(double newEdgeWeight) { this.newEdgeWeight = newEdgeWeight; }
    public void setOnStatusChange(Consumer<String> onStatusChange) { this.onStatusChange = onStatusChange; }
    public void setOnGraphChanged(Runnable onGraphChanged) { this.onGraphChanged = onGraphChanged; }
    public void setOnNodeRightClicked(Consumer<Node> onNodeRightClicked) { this.onNodeRightClicked = onNodeRightClicked; }

    private void status(String message) {
        if (onStatusChange != null) {
            onStatusChange.accept(message);
        }
    }

    private void fireGraphChanged() {
        if (onGraphChanged != null) {
            onGraphChanged.run();
        }
        redraw();
    }

    private static final class DijkstraEdgeMatcher {
        private DijkstraEdgeMatcher() {}

        static boolean isPathEdge(AlgorithmResult result, Edge edge) {
            if (result == null || result.getPath().size() < 2) return false;
            Set<String> pairs = new HashSet<>();
            for (int i = 0; i < result.getPath().size() - 1; i++) {
                int a = result.getPath().get(i).getId();
                int b = result.getPath().get(i + 1).getId();
                pairs.add(a + ":" + b);
                pairs.add(b + ":" + a);
            }
            return pairs.contains(edge.getSource().getId() + ":" + edge.getTarget().getId());
        }
    }
}
