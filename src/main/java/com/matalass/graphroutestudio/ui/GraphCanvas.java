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
    private static final Color BG = Color.web("#161A24");
    private static final Color NODE_DEFAULT = Color.web("#5EA3F3");
    private static final Color NODE_VISITED = Color.web("#7AD97A");
    private static final Color NODE_ACTIVE = Color.web("#FFD166");
    private static final Color NODE_PATH = Color.web("#EF476F");
    private static final Color NODE_CYCLE = Color.web("#FF7B72");
    private static final Color EDGE_DEFAULT = Color.web("#54617F");
    private static final Color EDGE_PATH = Color.web("#EF476F");
    private static final Color EDGE_MST = Color.web("#7AD97A");
    private static final Color LABEL_COLOR = Color.web("#A9B4D0");
    private static final Color CARD_BG = Color.web("#0E1320", 0.92);

    public enum EditMode { VIEW, EDIT, ADD_EDGE }

    private Graph graph;
    private EditMode mode = EditMode.VIEW;
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
    private Runnable onSelectionChanged;
    private Consumer<Node> onNodeRightClicked;

    public GraphCanvas(double width, double height) {
        super(width, height);
        setupInteractions();
    }

    private void setupInteractions() {
        setOnMousePressed(event -> {
            if (graph == null) {
                return;
            }
            Node hit = nodeAt(event.getX(), event.getY());

            if (event.getButton() == MouseButton.PRIMARY) {
                handlePrimaryClick(hit, event.getX(), event.getY());
            } else if (event.getButton() == MouseButton.SECONDARY) {
                handleSecondaryClick(hit);
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
            if (graph == null || event.getClickCount() != 2 || mode != EditMode.EDIT) {
                return;
            }
            Node hit = nodeAt(event.getX(), event.getY());
            if (hit != null) {
                graph.removeNode(hit.getId());
                if (selectedSourceId != null && selectedSourceId == hit.getId()) selectedSourceId = null;
                if (selectedTargetId != null && selectedTargetId == hit.getId()) selectedTargetId = null;
                status("Nœud supprimé : " + hit.getLabel());
                fireSelectionChanged();
                fireGraphChanged();
            }
            redraw();
        });
    }

    private void handlePrimaryClick(Node hit, double x, double y) {
        if (mode == EditMode.VIEW) {
            if (hit != null) {
                cycleSelection(hit);
            } else {
                status("Mode vue : cliquez sur un nœud pour définir source puis cible");
            }
            return;
        }

        if (hit != null) {
            if (mode == EditMode.ADD_EDGE) {
                if (edgeSourceNode == null) {
                    edgeSourceNode = hit;
                    status("Ajout d'arc : source = " + hit.getLabel() + " — cliquez sur la cible");
                } else if (!edgeSourceNode.equals(hit)) {
                    graph.addEdge(edgeSourceNode.getId(), hit.getId(), newEdgeWeight);
                    status("Arc ajouté : " + edgeSourceNode.getLabel() + " → " + hit.getLabel() + " (" + format(newEdgeWeight) + ")");
                    edgeSourceNode = null;
                    fireGraphChanged();
                }
                return;
            }

            if (mode == EditMode.EDIT) {
                draggingNode = hit;
                dragOffsetX = x - hit.getX();
                dragOffsetY = y - hit.getY();
            }
            return;
        }

        if (mode == EditMode.EDIT) {
            Node node = graph.addNode(x, y);
            status("Nœud " + node.getLabel() + " créé");
            fireGraphChanged();
        }
    }

    private void handleSecondaryClick(Node hit) {
        if (mode == EditMode.VIEW) {
            if (hit == null) {
                clearSelections();
                status("Sélection source/cible effacée");
            } else if (selectedSourceId == null) {
                selectedSourceId = hit.getId();
                selectedTargetId = null;
                status("Source fixée : " + hit.getLabel());
            } else {
                selectedTargetId = hit.getId();
                status("Cible fixée : " + hit.getLabel());
            }
            fireSelectionChanged();
            return;
        }

        if (hit != null && onNodeRightClicked != null) {
            onNodeRightClicked.accept(hit);
        }
    }

    private void cycleSelection(Node node) {
        if (selectedSourceId == null) {
            selectedSourceId = node.getId();
            selectedTargetId = null;
            status("Source sélectionnée : " + node.getLabel() + " — cliquez maintenant sur la cible");
        } else if (selectedTargetId == null) {
            if (selectedSourceId.equals(node.getId())) {
                status("La cible doit être différente de la source");
                return;
            }
            selectedTargetId = node.getId();
            status("Trajet prêt : " + labelOf(selectedSourceId) + " → " + node.getLabel());
        } else {
            selectedSourceId = node.getId();
            selectedTargetId = null;
            status("Nouvelle source : " + node.getLabel() + " — cliquez maintenant sur la cible");
        }
        fireSelectionChanged();
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
        drawSelectionCard(gc);
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

        boolean highlighted = isPathEdge(edge) || isActiveAnimationEdge(edge);
        gc.setStroke(resolveEdgeColor(edge));
        gc.setLineWidth(highlighted ? 4.2 : 2.5);
        gc.strokeLine(sx, sy, tx, ty);
        if (graph.isDirected()) {
            drawArrow(gc, tx, ty, angle);
        }

        gc.setFill(highlighted ? Color.WHITE : LABEL_COLOR);
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, highlighted ? 11.5 : 11));
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
        gc.setLineWidth(isSelected(node.getId()) ? 3.2 : 1.6);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(node.getLabel(), x, y + 4);

        if (hasDistance(node.getId())) {
            gc.setFill(Color.web("#D3DCED"));
            gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
            gc.fillText(distanceText(node.getId()), x, y + NODE_RADIUS + 14);
        }

        if (selectedSourceId != null && selectedSourceId == node.getId()) {
            drawSelectionBadge(gc, x - 18, y - 28, "S", Color.web("#06D6A0"));
        }
        if (selectedTargetId != null && selectedTargetId == node.getId()) {
            drawSelectionBadge(gc, x + 6, y - 28, "T", Color.web("#EF476F"));
        }
    }

    private void drawSelectionBadge(GraphicsContext gc, double x, double y, String text, Color color) {
        gc.setFill(color);
        gc.fillRoundRect(x, y, 18, 16, 8, 8);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x + 9, y + 11.5);
    }

    private void drawLegend(GraphicsContext gc) {
        gc.setFill(CARD_BG);
        gc.fillRoundRect(12, getHeight() - 112, 310, 94, 16, 16);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Mode " + modeLabel(), 24, getHeight() - 84);
        gc.setFill(Color.web("#D3DCED"));
        gc.setFont(Font.font("System", 11));
        gc.fillText(modeInstructionsLine1(), 24, getHeight() - 64);
        gc.fillText(modeInstructionsLine2(), 24, getHeight() - 46);
        gc.fillText("Double-clic en édition : supprimer un nœud", 24, getHeight() - 28);
    }

    private void drawSelectionCard(GraphicsContext gc) {
        gc.setFill(CARD_BG);
        gc.fillRoundRect(14, 14, 292, 88, 16, 16);
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.fillText("Sélection courante", 26, 36);

        gc.setFont(Font.font("System", 11));
        gc.setFill(Color.web("#06D6A0"));
        gc.fillText("Source : " + labelOf(selectedSourceId), 26, 58);
        gc.setFill(Color.web("#EF476F"));
        gc.fillText("Cible : " + labelOf(selectedTargetId), 26, 78);
        gc.setFill(Color.web("#D3DCED"));
        gc.fillText(currentResult == null ? "Aucun résultat affiché" : currentResult.getAlgorithmName() + " prêt à être animé", 150, 58);
        gc.fillText(graph == null ? "" : graph.getName(), 150, 78);
    }

    private Color resolveNodeColor(Node node) {
        int id = node.getId();
        if (animationStep != null) {
            if (animationStep.pathNodes().contains(id)) return NODE_PATH;
            if (animationStep.activeNodes().contains(id)) return NODE_ACTIVE;
            if (animationStep.visitedNodes().contains(id)) return NODE_VISITED;
        }
        if (currentResult != null && currentResult.getCycleNodes().stream().anyMatch(n -> n.getId() == id)) {
            return NODE_CYCLE;
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

    private boolean isActiveAnimationEdge(Edge edge) {
        return animationStep != null && animationStep.pathEdgeIds().contains(edge.getId());
    }

    private String formatEdgeLabel(Edge edge) {
        if (currentResult != null && currentResult.getFlowMap().containsKey(edge) && currentResult.getFlowMap().get(edge) > 0) {
            return format(edge.getWeight()) + " | f=" + currentResult.getFlowMap().get(edge);
        }
        return graph != null && graph.isWeighted() ? format(edge.getWeight()) : "";
    }

    private boolean hasDistance(int nodeId) {
        if (animationStep != null && animationStep.currentDistances().containsKey(nodeId)) {
            return animationStep.currentDistances().get(nodeId) != Double.MAX_VALUE;
        }
        return currentResult != null
                && currentResult.getType() == AlgorithmResult.Type.SHORTEST_PATH
                && currentResult.getDistances().containsKey(nodeId)
                && currentResult.getDistances().get(nodeId) != Double.MAX_VALUE;
    }

    private String distanceText(int nodeId) {
        if (animationStep != null && animationStep.currentDistances().containsKey(nodeId)) {
            return format(animationStep.currentDistances().get(nodeId));
        }
        if (currentResult != null && currentResult.getDistances().containsKey(nodeId)) {
            return format(currentResult.getDistances().get(nodeId));
        }
        return "";
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
        fireSelectionChanged();
        redraw();
    }

    public void swapSelections() {
        if (selectedSourceId == null || selectedTargetId == null) {
            return;
        }
        int tmp = selectedSourceId;
        selectedSourceId = selectedTargetId;
        selectedTargetId = tmp;
        fireSelectionChanged();
        redraw();
    }

    public Graph getGraph() { return graph; }
    public EditMode getMode() { return mode; }
    public void setMode(EditMode mode) {
        this.mode = mode;
        edgeSourceNode = null;
        status("Mode : " + modeLabel());
        redraw();
    }
    public Integer getSelectedSourceId() { return selectedSourceId; }
    public Integer getSelectedTargetId() { return selectedTargetId; }
    public void setSelectedSource(Integer selectedSourceId) { this.selectedSourceId = selectedSourceId; fireSelectionChanged(); redraw(); }
    public void setSelectedTarget(Integer selectedTargetId) { this.selectedTargetId = selectedTargetId; fireSelectionChanged(); redraw(); }
    public void setNewEdgeWeight(double newEdgeWeight) { this.newEdgeWeight = newEdgeWeight; }
    public void setOnStatusChange(Consumer<String> onStatusChange) { this.onStatusChange = onStatusChange; }
    public void setOnGraphChanged(Runnable onGraphChanged) { this.onGraphChanged = onGraphChanged; }
    public void setOnSelectionChanged(Runnable onSelectionChanged) { this.onSelectionChanged = onSelectionChanged; }
    public void setOnNodeRightClicked(Consumer<Node> onNodeRightClicked) { this.onNodeRightClicked = onNodeRightClicked; }

    private void status(String message) {
        if (onStatusChange != null) {
            onStatusChange.accept(message);
        }
    }

    private void fireSelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    private void fireGraphChanged() {
        if (onGraphChanged != null) {
            onGraphChanged.run();
        }
        redraw();
    }

    private String labelOf(Integer nodeId) {
        if (graph == null || nodeId == null) {
            return "—";
        }
        return graph.getNode(nodeId).map(Node::getLabel).orElse("—");
    }

    private String modeLabel() {
        return switch (mode) {
            case VIEW -> "Vue / sélection";
            case EDIT -> "Édition";
            case ADD_EDGE -> "Ajout d'arc";
        };
    }

    private String modeInstructionsLine1() {
        return switch (mode) {
            case VIEW -> "Clic gauche : source puis cible. Clic droit vide : effacer.";
            case EDIT -> "Clic vide : créer un nœud. Glisser : déplacer.";
            case ADD_EDGE -> "Clic : choisir la source puis la cible du nouvel arc.";
        };
    }

    private String modeInstructionsLine2() {
        return switch (mode) {
            case VIEW -> "Les résultats affichent aussi les distances sur les nœuds.";
            case EDIT -> "Clic droit : définir rapidement source/cible si nécessaire.";
            case ADD_EDGE -> "Poids défini dans le panneau latéral.";
        };
    }

    private String format(double value) {
        if (value == Double.MAX_VALUE) {
            return "∞";
        }
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
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
