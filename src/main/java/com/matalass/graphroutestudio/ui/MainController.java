package com.matalass.graphroutestudio.ui;

import com.matalass.graphroutestudio.algorithms.AStar;
import com.matalass.graphroutestudio.algorithms.AlgorithmResult;
import com.matalass.graphroutestudio.algorithms.BellmanFord;
import com.matalass.graphroutestudio.algorithms.CycleDetector;
import com.matalass.graphroutestudio.algorithms.Dijkstra;
import com.matalass.graphroutestudio.algorithms.FloydWarshall;
import com.matalass.graphroutestudio.algorithms.FordFulkerson;
import com.matalass.graphroutestudio.algorithms.KruskalPrim;
import com.matalass.graphroutestudio.analysis.GraphAnalyzer;
import com.matalass.graphroutestudio.analysis.GraphMetrics;
import com.matalass.graphroutestudio.animation.AnimationController;
import com.matalass.graphroutestudio.engine.Graph;
import com.matalass.graphroutestudio.engine.Node;
import com.matalass.graphroutestudio.io.GraphIO;
import com.matalass.graphroutestudio.presets.GraphPresets;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainController {
    private final Stage stage;
    private Graph graph;
    private GraphCanvas canvas;
    private AnimationController animCtrl;

    private TextArea logArea;
    private Label statusLabel;
    private Label graphInfoLabel;
    private Label stepDescLabel;
    private Label stepCountLabel;
    private Slider stepSlider;
    private ComboBox<String> algoCombo;
    private Label metricsLabel;

    private AlgorithmResult lastResult;

    private static final String[] ALGORITHMS = {
            "Dijkstra", "A*", "Bellman-Ford", "Floyd-Warshall",
            "Kruskal (MST)", "Prim (MST)", "Détection de cycles", "Flot max (Ford-Fulkerson)"
    };

    public MainController(Stage stage) {
        this.stage = stage;
        this.graph = GraphPresets.transportNetwork(760, 600);
    }

    public BorderPane buildUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1A1D27;");
        root.setTop(buildMenuBar());

        canvas = new GraphCanvas(760, 600);
        canvas.setGraph(graph);
        canvas.setOnStatusChange(message -> Platform.runLater(() -> statusLabel.setText(message)));
        canvas.setOnGraphChanged(() -> Platform.runLater(this::updateGraphInfo));
        canvas.setOnNodeRightClicked(node -> Platform.runLater(() -> onNodeRightClicked(node)));

        animCtrl = new AnimationController(canvas);
        animCtrl.setOnStepChange(() -> Platform.runLater(this::updateStepUI));

        StackPane center = new StackPane(canvas);
        center.setStyle("-fx-background-color: #1A1D27;");
        root.setCenter(center);
        root.setRight(buildRightPanel());
        root.setBottom(buildStatusBar());

        updateGraphInfo();
        statusLabel.setText("Preset transport chargé — clic droit pour sélectionner source/cible");
        return root;
    }

    private MenuBar buildMenuBar() {
        MenuBar bar = new MenuBar();
        Menu fileMenu = new Menu("Fichier");
        MenuItem newGraph = new MenuItem("Nouveau");
        MenuItem openJson = new MenuItem("Ouvrir JSON");
        MenuItem saveJson = new MenuItem("Sauvegarder JSON");
        MenuItem importTxt = new MenuItem("Importer tableau contraintes");
        MenuItem quit = new MenuItem("Quitter");
        newGraph.setOnAction(event -> resetGraph());
        openJson.setOnAction(event -> openJson());
        saveJson.setOnAction(event -> saveJson());
        importTxt.setOnAction(event -> importConstraints());
        quit.setOnAction(event -> Platform.exit());
        fileMenu.getItems().addAll(newGraph, openJson, saveJson, importTxt, SeparatorMenuItemCompat.create(), quit);

        Menu presetMenu = new Menu("Presets");
        MenuItem transport = new MenuItem("Réseau de transport / GPS");
        MenuItem demo = new MenuItem("Réseau pondéré démo");
        MenuItem randomSmall = new MenuItem("Aléatoire — 8 nœuds");
        MenuItem randomMedium = new MenuItem("Aléatoire — 14 nœuds");
        transport.setOnAction(event -> applyGraph(GraphPresets.transportNetwork(canvas.getWidth(), canvas.getHeight()), "Preset transport chargé"));
        demo.setOnAction(event -> applyGraph(GraphPresets.demoWeightedNetwork(canvas.getWidth(), canvas.getHeight()), "Preset démo chargé"));
        randomSmall.setOnAction(event -> applyGraph(GraphIO.generateRandom(8, 0.35, true, true, canvas.getWidth(), canvas.getHeight()), "Graphe aléatoire 8 nœuds"));
        randomMedium.setOnAction(event -> applyGraph(GraphIO.generateRandom(14, 0.22, true, true, canvas.getWidth(), canvas.getHeight()), "Graphe aléatoire 14 nœuds"));
        presetMenu.getItems().addAll(transport, demo, randomSmall, randomMedium);

        Menu modeMenu = new Menu("Mode");
        MenuItem edit = new MenuItem("Édition");
        MenuItem addEdge = new MenuItem("Ajouter arc");
        MenuItem view = new MenuItem("Vue");
        edit.setOnAction(event -> canvas.setMode(GraphCanvas.EditMode.EDIT));
        addEdge.setOnAction(event -> canvas.setMode(GraphCanvas.EditMode.ADD_EDGE));
        view.setOnAction(event -> canvas.setMode(GraphCanvas.EditMode.VIEW));
        modeMenu.getItems().addAll(edit, addEdge, view);

        Menu graphMenu = new Menu("Graphe");
        CheckMenuItem directed = new CheckMenuItem("Orienté");
        CheckMenuItem weighted = new CheckMenuItem("Pondéré");
        directed.setSelected(graph.isDirected());
        weighted.setSelected(graph.isWeighted());
        directed.setOnAction(event -> { graph.setDirected(directed.isSelected()); canvas.redraw(); updateGraphInfo(); });
        weighted.setOnAction(event -> { graph.setWeighted(weighted.isSelected()); canvas.redraw(); updateGraphInfo(); });
        graphMenu.getItems().addAll(directed, weighted);

        bar.getMenus().addAll(fileMenu, presetMenu, modeMenu, graphMenu);
        return bar;
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(360);
        panel.setStyle("-fx-background-color: #252836;");

        Label title = new Label("GraphRoute Studio");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#E2E8F0"));

        panel.getChildren().addAll(
                title,
                new Separator(),
                buildAlgorithmPanel(),
                new Separator(),
                buildMetricsPanel(),
                new Separator(),
                buildAnimationPanel(),
                new Separator(),
                buildLogPanel()
        );
        return panel;
    }

    private VBox buildAlgorithmPanel() {
        VBox box = new VBox(8);
        algoCombo = new ComboBox<>();
        algoCombo.getItems().addAll(ALGORITHMS);
        algoCombo.setValue(ALGORITHMS[0]);
        algoCombo.setMaxWidth(Double.MAX_VALUE);

        Label instruction = styled("Clic droit sur les nœuds pour définir source puis cible.", false);
        instruction.setWrapText(true);

        HBox badgeRow = new HBox(8, badge("Source", "#06D6A0"), badge("Cible", "#EF476F"));
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        HBox weightRow = new HBox(8);
        weightRow.setAlignment(Pos.CENTER_LEFT);
        Spinner<Double> weightSpinner = new Spinner<>(0.1, 999.0, 1.0, 0.5);
        weightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> canvas.setNewEdgeWeight(newVal));
        weightRow.getChildren().addAll(styled("Poids nouvel arc :", false), weightSpinner);

        Button runBtn = actionButton("▶ Exécuter", "#4A90D9", this::runAlgorithm);
        Button clearBtn = actionButton("Réinitialiser vue", "#4A5568", () -> {
            graph.clearVisualState();
            canvas.showResult(null);
            logArea.clear();
            lastResult = null;
            updateGraphInfo();
        });

        box.getChildren().addAll(styled("Algorithmes", true), algoCombo, instruction, badgeRow, weightRow, runBtn, clearBtn);
        return box;
    }

    private VBox buildMetricsPanel() {
        VBox box = new VBox(8);
        metricsLabel = new Label();
        metricsLabel.setWrapText(true);
        metricsLabel.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 12;");
        box.getChildren().addAll(styled("Diagnostic rapide", true), metricsLabel);
        return box;
    }

    private VBox buildAnimationPanel() {
        VBox box = new VBox(8);
        stepDescLabel = new Label("—");
        stepDescLabel.setWrapText(true);
        stepDescLabel.setStyle("-fx-text-fill: #FFD166; -fx-font-size: 11;");
        stepSlider = new Slider(0, 0, 0);
        stepSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (stepSlider.isValueChanging()) {
                animCtrl.goToStep(newVal.intValue());
            }
        });
        stepCountLabel = new Label("Étape 0 / 0");
        stepCountLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 11;");

        HBox controls = new HBox(6);
        controls.setAlignment(Pos.CENTER_LEFT);
        Button stopBtn = smallButton("⏹", "#EF476F", animCtrl::stop);
        Button backBtn = smallButton("⏮", "#4A5568", animCtrl::stepBack);
        Button playBtn = smallButton("▶", "#6BCB77", () -> { if (animCtrl.isPlaying()) animCtrl.pause(); else animCtrl.play(); });
        Button fwdBtn = smallButton("⏭", "#4A5568", animCtrl::stepForward);
        animCtrl.playingProperty().addListener((obs, oldVal, newVal) -> playBtn.setText(newVal ? "⏸" : "▶"));

        Slider speedSlider = new Slider(0.2, 5.0, 1.0);
        speedSlider.valueProperty().bindBidirectional(animCtrl.speedProperty());
        controls.getChildren().addAll(stopBtn, backBtn, playBtn, fwdBtn, styled("Vitesse", false), speedSlider);

        animCtrl.currentStepProperty().addListener((obs, oldVal, newVal) -> updateStepUI());
        animCtrl.totalStepsProperty().addListener((obs, oldVal, newVal) -> stepSlider.setMax(Math.max(0, newVal.intValue() - 1)));

        box.getChildren().addAll(styled("Animation pas-à-pas", true), stepDescLabel, stepSlider, stepCountLabel, controls);
        return box;
    }

    private ScrollPane buildLogPanel() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(14);
        logArea.setStyle("-fx-control-inner-background: #1A1D27; -fx-text-fill: #A0AEC0; -fx-font-family: 'Monospaced'; -fx-font-size: 11;");
        ScrollPane pane = new ScrollPane(logArea);
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        VBox.setVgrow(pane, Priority.ALWAYS);
        return pane;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setStyle("-fx-background-color: #0D0F19;");
        bar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("Prêt");
        statusLabel.setStyle("-fx-text-fill: #A0AEC0;");
        graphInfoLabel = new Label();
        graphInfoLabel.setStyle("-fx-text-fill: #718096;");
        bar.getChildren().addAll(statusLabel, new Separator(Orientation.VERTICAL), graphInfoLabel);
        return bar;
    }

    private void runAlgorithm() {
        if (graph.nodeCount() == 0) {
            alert("Le graphe est vide.");
            return;
        }
        String algo = algoCombo.getValue();
        AlgorithmResult result;
        try {
            result = switch (algo) {
                case "Dijkstra" -> runWithSourceTarget(Dijkstra::run);
                case "A*" -> runWithSourceTarget(AStar::run);
                case "Bellman-Ford" -> runWithSourceTarget(BellmanFord::run);
                case "Floyd-Warshall" -> FloydWarshall.run(graph);
                case "Kruskal (MST)" -> KruskalPrim.kruskal(graph);
                case "Prim (MST)" -> runWithSource(start -> KruskalPrim.prim(graph, start));
                case "Détection de cycles" -> CycleDetector.run(graph);
                case "Flot max (Ford-Fulkerson)" -> runWithSourceTarget(FordFulkerson::run);
                default -> throw new IllegalStateException("Algorithme inconnu");
            };
        } catch (IllegalArgumentException ex) {
            alert(ex.getMessage());
            return;
        }

        lastResult = result;
        logArea.setText(result.getLog());
        if (!result.isSuccess()) {
            canvas.showResult(null);
            alert(result.getErrorMessage());
            return;
        }

        canvas.showResult(result);
        animCtrl.load(result);
        statusLabel.setText(algo + " terminé — " + result.getSteps().size() + " étapes");
        appendSummary(result);
        updateGraphInfo();
    }

    @FunctionalInterface
    interface BiAlgo { AlgorithmResult run(Graph graph, int src, int tgt); }
    @FunctionalInterface
    interface UniAlgo { AlgorithmResult run(int src); }

    private AlgorithmResult runWithSourceTarget(BiAlgo algo) {
        Integer src = canvas.getSelectedSourceId();
        Integer tgt = canvas.getSelectedTargetId();
        if (src == null || tgt == null) {
            throw new IllegalArgumentException("Sélectionnez une source ET une cible avec clic droit.");
        }
        return algo.run(graph, src, tgt);
    }

    private AlgorithmResult runWithSource(UniAlgo algo) {
        Integer src = canvas.getSelectedSourceId();
        if (src == null) {
            src = graph.getNodes().iterator().next().getId();
        }
        return algo.run(src);
    }

    private void appendSummary(AlgorithmResult result) {
        StringBuilder sb = new StringBuilder("\n\n─── Résumé ───\n");
        switch (result.getType()) {
            case SHORTEST_PATH -> {
                sb.append("Chemin : ");
                for (int i = 0; i < result.getPath().size(); i++) {
                    if (i > 0) sb.append(" → ");
                    sb.append(result.getPath().get(i).getLabel());
                }
                sb.append("\nCoût : ").append(fmt(result.getPathWeight()));
            }
            case SPANNING_TREE -> sb.append("Poids MST : ").append(fmt(result.getSpanningTreeWeight()));
            case CYCLE_DETECTION -> sb.append("Cycle : ").append(result.hasCycle() ? "détecté" : "absent");
            case MAX_FLOW -> sb.append("Flot maximal : ").append(result.getMaxFlow());
            case ALL_PAIRS_SHORTEST -> sb.append("Matrice calculée pour ").append(graph.nodeCount()).append(" nœuds");
        }
        logArea.appendText(sb.toString());
    }

    private void onNodeRightClicked(Node node) {
        Integer src = canvas.getSelectedSourceId();
        if (src == null || src.equals(canvas.getSelectedTargetId())) {
            canvas.setSelectedSource(node.getId());
            canvas.setSelectedTarget(null);
            statusLabel.setText("Source : " + node.getLabel() + " — choisissez la cible");
        } else {
            canvas.setSelectedTarget(node.getId());
            statusLabel.setText("Trajet sélectionné : " + graph.getNode(src).orElseThrow().getLabel() + " → " + node.getLabel());
        }
    }

    private void resetGraph() {
        applyGraph(new Graph(true, true, "Nouveau graphe"), "Nouveau graphe créé");
    }

    private void applyGraph(Graph nextGraph, String status) {
        this.graph = nextGraph;
        this.lastResult = null;
        canvas.setGraph(graph);
        logArea.clear();
        animCtrl.stop();
        updateGraphInfo();
        statusLabel.setText(status);
    }

    private void openJson() {
        File file = fileChooser("JSON", "*.json").showOpenDialog(stage);
        if (file == null) return;
        try {
            applyGraph(GraphIO.loadJson(file), "Graphe chargé : " + file.getName());
        } catch (Exception ex) {
            alert("Erreur de lecture : " + ex.getMessage());
        }
    }

    private void saveJson() {
        File file = fileChooser("JSON", "*.json").showSaveDialog(stage);
        if (file == null) return;
        try {
            GraphIO.saveJson(graph, file);
            statusLabel.setText("Graphe sauvegardé : " + file.getName());
        } catch (Exception ex) {
            alert("Erreur d'écriture : " + ex.getMessage());
        }
    }

    private void importConstraints() {
        File file = fileChooser("Texte", "*.txt").showOpenDialog(stage);
        if (file == null) return;
        try {
            applyGraph(GraphIO.loadConstraintTable(file, canvas.getWidth(), canvas.getHeight()), "Tableau de contraintes importé : " + file.getName());
        } catch (Exception ex) {
            alert("Erreur d'import : " + ex.getMessage());
        }
    }

    private void updateGraphInfo() {
        GraphMetrics metrics = GraphAnalyzer.analyze(graph);
        graphInfoLabel.setText(graph.getName() + " | " + metrics.nodeCount() + " nœuds | " + metrics.edgeCount() + " arcs");
        metricsLabel.setText(String.join("\n",
                "Orienté : " + yesNo(metrics.directed()),
                "Pondéré : " + yesNo(metrics.weighted()),
                "Connecté (faible) : " + yesNo(metrics.connected()),
                "Poids négatifs : " + yesNo(metrics.hasNegativeWeight()),
                "Densité : " + fmt(metrics.density()),
                "Degré moyen : " + fmt(metrics.averageDegree()),
                "Poids total : " + fmt(metrics.totalWeight()),
                lastResult != null && lastResult.getType() == AlgorithmResult.Type.SHORTEST_PATH
                        ? "Dernier coût chemin : " + fmt(lastResult.getPathWeight())
                        : ""
        ));
    }

    private void updateStepUI() {
        int current = animCtrl.getCurrentStep();
        int total = animCtrl.getTotalSteps();
        stepCountLabel.setText("Étape " + (total == 0 ? 0 : current + 1) + " / " + total);
        stepDescLabel.setText(animCtrl.getCurrentDescription());
        if (total > 0) {
            stepSlider.setValue(current);
        }
    }

    private Alert alertDialog(String message) {
        Alert dlg = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        dlg.setHeaderText(null);
        dlg.setTitle("Erreur");
        return dlg;
    }

    private void alert(String message) {
        alertDialog(message).showAndWait();
    }

    private FileChooser fileChooser(String desc, String ext) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, ext));
        return chooser;
    }

    private Label styled(String text, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(Color.web(bold ? "#E2E8F0" : "#A0AEC0"));
        if (bold) label.setFont(Font.font("System", FontWeight.BOLD, 13));
        return label;
    }

    private Label badge(String text, String color) {
        Label label = new Label("● " + text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11; -fx-font-weight: bold;");
        return label;
    }

    private Button actionButton(String text, String color, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button smallButton(String text, String color, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-min-width: 36; -fx-background-radius: 6;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private String fmt(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }

    private String yesNo(boolean value) {
        return value ? "oui" : "non";
    }

    private static final class SeparatorMenuItemCompat {
        private SeparatorMenuItemCompat() {}
        static javafx.scene.control.SeparatorMenuItem create() { return new javafx.scene.control.SeparatorMenuItem(); }
    }
}
