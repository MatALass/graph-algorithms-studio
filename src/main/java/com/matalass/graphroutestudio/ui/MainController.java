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
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    private static final String CARD_STYLE = "-fx-background-color: #202636; -fx-background-radius: 14; -fx-padding: 14;"
            + " -fx-border-color: #334155; -fx-border-radius: 14;";
    private static final String[] ALGORITHMS = {
            "Dijkstra", "A*", "Bellman-Ford", "Floyd-Warshall",
            "Kruskal (MST)", "Prim (MST)", "Détection de cycles", "Flot max (Ford-Fulkerson)"
    };

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
    private Label algorithmHintLabel;
    private Label selectionSourceLabel;
    private Label selectionTargetLabel;
    private Label resultTitleLabel;
    private Label resultSummaryLabel;
    private Label resultDetailsLabel;

    private AlgorithmResult lastResult;

    public MainController(Stage stage) {
        this.stage = stage;
        this.graph = GraphPresets.transportNetwork(760, 600);
    }

    public BorderPane buildUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #161A24;");
        root.setTop(buildMenuBar());

        canvas = new GraphCanvas(840, 660);
        canvas.setGraph(graph);
        canvas.setOnStatusChange(message -> Platform.runLater(() -> statusLabel.setText(message)));
        canvas.setOnGraphChanged(() -> Platform.runLater(this::handleGraphChanged));
        canvas.setOnSelectionChanged(() -> Platform.runLater(this::updateSelectionInfo));
        canvas.setOnNodeRightClicked(node -> Platform.runLater(() -> onNodeRightClicked(node)));

        animCtrl = new AnimationController(canvas);
        animCtrl.setOnStepChange(() -> Platform.runLater(this::updateStepUI));

        StackPane center = new StackPane(canvas);
        center.setStyle("-fx-background-color: #161A24; -fx-padding: 12;");
        root.setCenter(center);
        root.setRight(buildRightPanel());
        root.setBottom(buildStatusBar());

        handleGraphChanged();
        updateSelectionInfo();
        renderEmptyResult();
        updateAlgorithmHint();
        statusLabel.setText("Preset transport chargé — mode vue : cliquez sur une source puis une cible");
        return root;
    }

    private MenuBar buildMenuBar() {
        MenuBar bar = new MenuBar();

        Menu fileMenu = new Menu("Fichier");
        MenuItem newGraph = new MenuItem("Nouveau graphe");
        MenuItem openJson = new MenuItem("Ouvrir JSON");
        MenuItem saveJson = new MenuItem("Sauvegarder JSON");
        MenuItem importTxt = new MenuItem("Importer tableau de contraintes");
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
        MenuItem view = new MenuItem("Vue / sélection");
        MenuItem edit = new MenuItem("Édition");
        MenuItem addEdge = new MenuItem("Ajouter arc");
        view.setOnAction(event -> canvas.setMode(GraphCanvas.EditMode.VIEW));
        edit.setOnAction(event -> canvas.setMode(GraphCanvas.EditMode.EDIT));
        addEdge.setOnAction(event -> canvas.setMode(GraphCanvas.EditMode.ADD_EDGE));
        modeMenu.getItems().addAll(view, edit, addEdge);

        Menu graphMenu = new Menu("Graphe");
        CheckMenuItem directed = new CheckMenuItem("Orienté");
        CheckMenuItem weighted = new CheckMenuItem("Pondéré");
        directed.setSelected(graph.isDirected());
        weighted.setSelected(graph.isWeighted());
        directed.setOnAction(event -> {
            graph.setDirected(directed.isSelected());
            canvas.redraw();
            handleGraphChanged();
        });
        weighted.setOnAction(event -> {
            graph.setWeighted(weighted.isSelected());
            canvas.redraw();
            handleGraphChanged();
        });
        graphMenu.getItems().addAll(directed, weighted);

        bar.getMenus().addAll(fileMenu, presetMenu, modeMenu, graphMenu);
        return bar;
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(400);
        panel.setStyle("-fx-background-color: #1D2331;");

        Label title = new Label("GraphRoute Studio");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#F8FAFC"));

        Label subtitle = new Label("Visualiseur JavaFX d'algorithmes de graphes orienté réseau, GPS et démonstration pédagogique.");
        subtitle.setWrapText(true);
        subtitle.setTextFill(Color.web("#A9B4D0"));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(12,
                buildSelectionPanel(),
                buildAlgorithmPanel(),
                buildResultPanel(),
                buildMetricsPanel(),
                buildAnimationPanel(),
                buildLogPanel(),
                buildHelpPanel()
        );
        content.setFillWidth(true);

        scrollPane.setContent(content);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(title, subtitle, scrollPane);
        return panel;
    }

    private VBox buildSelectionPanel() {
        VBox box = card();
        Label title = sectionTitle("Sélection trajet");
        selectionSourceLabel = infoLabel();
        selectionTargetLabel = infoLabel();
        updateSelectionInfo();

        HBox actions = new HBox(8);
        Button clearSelection = secondaryButton("Effacer sélection", () -> {
            canvas.clearSelections();
            statusLabel.setText("Sélection source/cible effacée");
        });
        Button swapSelection = secondaryButton("Inverser", () -> {
            canvas.swapSelections();
            statusLabel.setText("Source et cible inversées");
        });
        actions.getChildren().addAll(clearSelection, swapSelection);

        Label hint = bodyLabel("Mode vue : clic gauche sur une source puis une cible. Clic droit dans le vide : réinitialiser.");
        hint.setWrapText(true);
        box.getChildren().addAll(title, selectionSourceLabel, selectionTargetLabel, actions, hint);
        return box;
    }

    private VBox buildAlgorithmPanel() {
        VBox box = card();
        algoCombo = new ComboBox<>();
        algoCombo.getItems().addAll(ALGORITHMS);
        algoCombo.setValue(ALGORITHMS[0]);
        algoCombo.setMaxWidth(Double.MAX_VALUE);
        algoCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateAlgorithmHint());

        algorithmHintLabel = bodyLabel("");
        algorithmHintLabel.setWrapText(true);

        Spinner<Double> weightSpinner = new Spinner<>();
        weightSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 999.0, 1.0, 0.5));
        weightSpinner.setEditable(true);
        weightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> canvas.setNewEdgeWeight(newVal));

        HBox weightRow = new HBox(8, bodyLabel("Poids nouvel arc"), weightSpinner);
        weightRow.setAlignment(Pos.CENTER_LEFT);

        HBox buttons = new HBox(8);
        Button runBtn = primaryButton("▶ Exécuter", this::runAlgorithm);
        Button clearBtn = secondaryButton("Réinitialiser vue", this::clearResultsAndVisualization);
        HBox.setHgrow(runBtn, Priority.ALWAYS);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        runBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        buttons.getChildren().addAll(runBtn, clearBtn);

        box.getChildren().addAll(sectionTitle("Algorithme"), algoCombo, algorithmHintLabel, weightRow, buttons);
        return box;
    }

    private VBox buildResultPanel() {
        VBox box = card();
        resultTitleLabel = sectionTitle("Résultat");
        resultSummaryLabel = infoLabel();
        resultSummaryLabel.setWrapText(true);
        resultDetailsLabel = bodyLabel("");
        resultDetailsLabel.setWrapText(true);
        box.getChildren().addAll(resultTitleLabel, resultSummaryLabel, resultDetailsLabel);
        return box;
    }

    private VBox buildMetricsPanel() {
        VBox box = card();
        metricsLabel = bodyLabel("");
        metricsLabel.setWrapText(true);
        box.getChildren().addAll(sectionTitle("Diagnostic rapide"), metricsLabel);
        return box;
    }

    private VBox buildAnimationPanel() {
        VBox box = card();
        stepDescLabel = bodyLabel("Aucune animation chargée.");
        stepDescLabel.setWrapText(true);
        stepCountLabel = captionLabel("Étape 0 / 0");
        stepSlider = new Slider(0, 0, 0);
        stepSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (stepSlider.isValueChanging()) {
                animCtrl.goToStep(newVal.intValue());
            }
        });
        stepSlider.setOnMouseReleased(event -> animCtrl.goToStep((int) Math.round(stepSlider.getValue())));

        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);
        Button stopBtn = iconButton("⏹", "Arrêter", animCtrl::stop, "#EF476F");
        Button backBtn = iconButton("⏮", "Étape précédente", animCtrl::stepBack, "#475569");
        Button playBtn = iconButton("▶", "Lecture / pause", () -> {
            if (animCtrl.isPlaying()) {
                animCtrl.pause();
            } else {
                animCtrl.play();
            }
        }, "#22C55E");
        Button fwdBtn = iconButton("⏭", "Étape suivante", animCtrl::stepForward, "#475569");
        animCtrl.playingProperty().addListener((obs, oldVal, playing) -> playBtn.setText(playing ? "⏸" : "▶"));

        Slider speedSlider = new Slider(0.2, 5.0, 1.0);
        speedSlider.valueProperty().bindBidirectional(animCtrl.speedProperty());
        HBox speedBox = new HBox(8, captionLabel("Vitesse"), speedSlider);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        controls.getChildren().addAll(stopBtn, backBtn, playBtn, fwdBtn);
        animCtrl.currentStepProperty().addListener((obs, oldVal, newVal) -> updateStepUI());
        animCtrl.totalStepsProperty().addListener((obs, oldVal, newVal) -> stepSlider.setMax(Math.max(0, newVal.intValue() - 1)));

        box.getChildren().addAll(sectionTitle("Animation pas-à-pas"), stepDescLabel, stepCountLabel, stepSlider, controls, speedBox);
        return box;
    }

    private VBox buildLogPanel() {
        VBox box = card();
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(14);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #121826; -fx-text-fill: #CBD5E1; -fx-font-family: 'Monospaced'; -fx-font-size: 11;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        box.getChildren().addAll(sectionTitle("Journal détaillé"), logArea);
        return box;
    }

    private VBox buildHelpPanel() {
        VBox box = card();
        Label help = bodyLabel(String.join("\n",
                "• Mode vue : sélection source/cible et lecture des résultats.",
                "• Mode édition : clic vide pour créer, glisser pour déplacer, double-clic pour supprimer.",
                "• Mode ajout d'arc : cliquez sur la source puis la cible.",
                "• Les distances calculées s'affichent sous les nœuds pour les algorithmes de plus court chemin."
        ));
        help.setWrapText(true);
        box.getChildren().addAll(sectionTitle("Guide express"), help);
        return box;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setStyle("-fx-background-color: #0D111A;");
        bar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = captionLabel("Prêt");
        graphInfoLabel = captionLabel("");
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
            animCtrl.stop();
            renderErrorResult(result.getErrorMessage());
            alert(result.getErrorMessage());
            return;
        }

        canvas.showResult(result);
        animCtrl.load(result);
        appendSummaryToLog(result);
        renderResult(result);
        statusLabel.setText(algo + " terminé — " + result.getSteps().size() + " étapes disponibles");
        handleGraphChanged();
    }

    @FunctionalInterface
    interface BiAlgo { AlgorithmResult run(Graph graph, int src, int tgt); }

    @FunctionalInterface
    interface UniAlgo { AlgorithmResult run(int src); }

    private AlgorithmResult runWithSourceTarget(BiAlgo algo) {
        Integer src = canvas.getSelectedSourceId();
        Integer tgt = canvas.getSelectedTargetId();
        if (src == null || tgt == null) {
            throw new IllegalArgumentException("Sélectionnez une source ET une cible en mode vue avant l'exécution.");
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

    private void appendSummaryToLog(AlgorithmResult result) {
        StringBuilder sb = new StringBuilder("\n\n─── Résumé ───\n");
        switch (result.getType()) {
            case SHORTEST_PATH -> {
                sb.append("Chemin : ").append(formatPath(result.getPath()));
                sb.append("\nCoût total : ").append(fmt(result.getPathWeight()));
            }
            case SPANNING_TREE -> {
                sb.append("Poids total : ").append(fmt(result.getSpanningTreeWeight()));
                sb.append("\nArêtes retenues : ").append(result.getSpanningTreeEdges().size());
            }
            case CYCLE_DETECTION -> sb.append("Cycle : ").append(result.hasCycle() ? "détecté" : "absent");
            case MAX_FLOW -> sb.append("Flot maximal : ").append(result.getMaxFlow());
            case ALL_PAIRS_SHORTEST -> sb.append("Matrice calculée pour ").append(graph.nodeCount()).append(" nœuds");
        }
        logArea.appendText(sb.toString());
    }

    private void renderResult(AlgorithmResult result) {
        resultTitleLabel.setText("Résultat — " + result.getAlgorithmName());
        switch (result.getType()) {
            case SHORTEST_PATH -> {
                resultSummaryLabel.setText("Chemin optimal : " + formatPath(result.getPath()));
                resultDetailsLabel.setText(String.join("\n",
                        "Coût total : " + fmt(result.getPathWeight()),
                        "Nœuds atteignables : " + reachableNodesCount(result.getDistances()) + " / " + graph.nodeCount(),
                        topDistancesText(result.getDistances())
                ));
            }
            case SPANNING_TREE -> {
                resultSummaryLabel.setText("Arbre couvrant trouvé avec " + result.getSpanningTreeEdges().size() + " arêtes.");
                resultDetailsLabel.setText(String.join("\n",
                        "Poids total : " + fmt(result.getSpanningTreeWeight()),
                        "Arêtes : " + result.getSpanningTreeEdges().stream()
                                .map(edge -> edge.getSource().getLabel() + "–" + edge.getTarget().getLabel())
                                .collect(Collectors.joining(", "))
                ));
            }
            case CYCLE_DETECTION -> {
                resultSummaryLabel.setText(result.hasCycle() ? "Cycle détecté dans le graphe." : "Aucun cycle détecté.");
                resultDetailsLabel.setText(result.hasCycle()
                        ? "Nœuds du cycle : " + result.getCycleNodes().stream().map(Node::getLabel).collect(Collectors.joining(" → "))
                        : "Le graphe peut être parcouru sans revenir sur un nœud dans une boucle fermée détectée.");
            }
            case MAX_FLOW -> {
                resultSummaryLabel.setText("Flot maximal : " + result.getMaxFlow());
                resultDetailsLabel.setText("Arcs avec flot non nul : " + result.getFlowMap().entrySet().stream()
                        .filter(entry -> entry.getValue() > 0)
                        .map(entry -> entry.getKey().getSource().getLabel() + "→" + entry.getKey().getTarget().getLabel() + "=" + entry.getValue())
                        .collect(Collectors.joining(", ")));
            }
            case ALL_PAIRS_SHORTEST -> {
                resultSummaryLabel.setText("Toutes les distances minimales ont été calculées.");
                resultDetailsLabel.setText(buildMatrixPreview(result));
            }
        }
    }

    private void renderErrorResult(String message) {
        resultTitleLabel.setText("Résultat");
        resultSummaryLabel.setText("Exécution interrompue.");
        resultDetailsLabel.setText(message);
    }

    private void renderEmptyResult() {
        resultTitleLabel.setText("Résultat");
        resultSummaryLabel.setText("Aucun algorithme exécuté.");
        resultDetailsLabel.setText("Sélectionnez une source et une cible si nécessaire, puis lancez un algorithme.");
    }

    private void onNodeRightClicked(Node node) {
        Integer src = canvas.getSelectedSourceId();
        if (src == null || src.equals(canvas.getSelectedTargetId())) {
            canvas.setSelectedSource(node.getId());
            canvas.setSelectedTarget(null);
            statusLabel.setText("Source fixée : " + node.getLabel() + " — choisissez la cible");
        } else {
            canvas.setSelectedTarget(node.getId());
            statusLabel.setText("Trajet sélectionné : " + labelOf(src) + " → " + node.getLabel());
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
        renderEmptyResult();
        updateSelectionInfo();
        handleGraphChanged();
        statusLabel.setText(status);
    }

    private void openJson() {
        File file = fileChooser("JSON", "*.json").showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            applyGraph(GraphIO.loadJson(file), "Graphe chargé : " + file.getName());
        } catch (Exception ex) {
            alert("Erreur de lecture : " + ex.getMessage());
        }
    }

    private void saveJson() {
        File file = fileChooser("JSON", "*.json").showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            GraphIO.saveJson(graph, file);
            statusLabel.setText("Graphe sauvegardé : " + file.getName());
        } catch (Exception ex) {
            alert("Erreur d'écriture : " + ex.getMessage());
        }
    }

    private void importConstraints() {
        File file = fileChooser("Texte", "*.txt").showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            applyGraph(GraphIO.loadConstraintTable(file, canvas.getWidth(), canvas.getHeight()), "Tableau de contraintes importé : " + file.getName());
        } catch (Exception ex) {
            alert("Erreur d'import : " + ex.getMessage());
        }
    }

    private void clearResultsAndVisualization() {
        graph.clearVisualState();
        canvas.showResult(null);
        animCtrl.stop();
        logArea.clear();
        lastResult = null;
        renderEmptyResult();
        handleGraphChanged();
        statusLabel.setText("Vue réinitialisée");
    }

    private void handleGraphChanged() {
        GraphMetrics metrics = GraphAnalyzer.analyze(graph);
        graphInfoLabel.setText(graph.getName() + " | " + metrics.nodeCount() + " nœuds | " + metrics.edgeCount() + " arcs | " + (graph.isDirected() ? "orienté" : "non orienté"));
        List<String> lines = new ArrayList<>();
        lines.add("Orienté : " + yesNo(metrics.directed()));
        lines.add("Pondéré : " + yesNo(metrics.weighted()));
        lines.add("Connecté (faible) : " + yesNo(metrics.connected()));
        lines.add("Poids négatifs : " + yesNo(metrics.hasNegativeWeight()));
        lines.add("Densité : " + fmt(metrics.density()));
        lines.add("Degré moyen : " + fmt(metrics.averageDegree()));
        lines.add("Poids total : " + fmt(metrics.totalWeight()));
        if (lastResult != null && lastResult.getType() == AlgorithmResult.Type.SHORTEST_PATH) {
            lines.add("Dernier coût chemin : " + fmt(lastResult.getPathWeight()));
        }
        metricsLabel.setText(String.join("\n", lines));
    }

    private void updateSelectionInfo() {
        selectionSourceLabel.setText("Source : " + labelOf(canvas != null ? canvas.getSelectedSourceId() : null));
        selectionTargetLabel.setText("Cible : " + labelOf(canvas != null ? canvas.getSelectedTargetId() : null));
    }

    private void updateAlgorithmHint() {
        if (algorithmHintLabel == null || algoCombo == null) {
            return;
        }
        String hint = switch (algoCombo.getValue()) {
            case "Dijkstra" -> "Idéal pour les poids non négatifs. Affiche le chemin optimal, le coût et les distances par nœud.";
            case "A*" -> "Version guidée par heuristique : pertinente pour des positions géographiques ou des cartes.";
            case "Bellman-Ford" -> "À privilégier dès qu'il y a des poids négatifs. Plus lent, mais plus général que Dijkstra.";
            case "Floyd-Warshall" -> "Calcule toutes les plus courtes distances entre tous les couples de nœuds.";
            case "Kruskal (MST)" -> "Construit un arbre couvrant minimum global du graphe non orienté pondéré.";
            case "Prim (MST)" -> "Construit un arbre couvrant minimum en partant d'une source.";
            case "Détection de cycles" -> "Vérifie rapidement si le graphe contient une boucle.";
            case "Flot max (Ford-Fulkerson)" -> "Utilise source et cible comme entrée/sortie pour calculer un flot maximal.";
            default -> "";
        };
        algorithmHintLabel.setText(hint);
    }

    private void updateStepUI() {
        int current = animCtrl.getCurrentStep();
        int total = animCtrl.getTotalSteps();
        stepCountLabel.setText("Étape " + (total == 0 ? 0 : current + 1) + " / " + total);
        String description = animCtrl.getCurrentDescription();
        stepDescLabel.setText(description == null || description.isBlank() ? "Aucune animation chargée." : description);
        if (!stepSlider.isValueChanging() && total > 0) {
            stepSlider.setValue(current);
        }
        if (total == 0) {
            stepSlider.setValue(0);
        }
    }

    private String buildMatrixPreview(AlgorithmResult result) {
        double[][] matrix = result.getDistMatrix();
        List<Node> nodes = result.getNodeOrder();
        if (matrix == null || nodes == null || nodes.isEmpty()) {
            return "Aperçu matrice indisponible.";
        }
        int limit = Math.min(4, nodes.size());
        StringBuilder sb = new StringBuilder("Aperçu matrice (premiers nœuds) :\n");
        for (int i = 0; i < limit; i++) {
            List<String> values = new ArrayList<>();
            for (int j = 0; j < limit; j++) {
                values.add(nodes.get(j).getLabel() + "=" + fmt(matrix[i][j]));
            }
            sb.append(nodes.get(i).getLabel()).append(" : ").append(String.join(" | ", values)).append("\n");
        }
        return sb.toString().trim();
    }

    private String topDistancesText(Map<Integer, Double> distances) {
        if (distances == null || distances.isEmpty()) {
            return "Distances : indisponibles";
        }
        String top = distances.entrySet().stream()
                .filter(entry -> entry.getValue() != Double.MAX_VALUE)
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(entry -> labelOf(entry.getKey()) + "=" + fmt(entry.getValue()))
                .collect(Collectors.joining(" · "));
        return top.isBlank() ? "Distances : aucune" : "Top distances : " + top;
    }

    private int reachableNodesCount(Map<Integer, Double> distances) {
        return (int) distances.values().stream().filter(value -> value != Double.MAX_VALUE).count();
    }

    private String formatPath(List<Node> path) {
        if (path == null || path.isEmpty()) {
            return "aucun chemin";
        }
        return path.stream().map(Node::getLabel).collect(Collectors.joining(" → "));
    }

    private String labelOf(Integer nodeId) {
        if (nodeId == null || graph == null) {
            return "—";
        }
        return graph.getNode(nodeId).map(Node::getLabel).orElse("—");
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

    private VBox card() {
        VBox box = new VBox(10);
        box.setStyle(CARD_STYLE);
        return box;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#F8FAFC"));
        label.setFont(Font.font("System", FontWeight.BOLD, 15));
        return label;
    }

    private Label infoLabel() {
        Label label = new Label();
        label.setTextFill(Color.web("#E2E8F0"));
        label.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        return label;
    }

    private Label bodyLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#A9B4D0"));
        label.setFont(Font.font("System", 12));
        return label;
    }

    private Label captionLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#94A3B8"));
        label.setFont(Font.font("System", 11));
        return label;
    }

    private Button primaryButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: linear-gradient(to right, #3B82F6, #2563EB); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 14;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button secondaryButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 14;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button iconButton(String text, String tooltip, Runnable action, String color) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-min-width: 42; -fx-min-height: 38;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private String fmt(double value) {
        if (value == Double.MAX_VALUE) return "∞";
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
