/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.view;

/**
 *
 * @author HP
 */

import com.monitorfx.model.ProcessRecord;
import com.monitorfx.model.SystemMetrics;
import com.monitorfx.util.Formatters;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Vue principale JavaFX du tableau de bord.
 * Cette classe gère uniquement la présentation et les interactions de l'interface.
 */
public class DashboardView {

    private static final DateTimeFormatter UI_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);

    private final BorderPane root;
    private final MetricCard ramCard;
    private final MetricCard cpuCard;
    private final VBox systemCard;

    private final Label runtimeValueLabel;
    private final Label platformValueLabel;
    private final Label threadsValueLabel;
    private final Label uptimeValueLabel;

    private final TableView<ProcessRecord> processTable;
    private final ObservableList<ProcessRecord> sourceProcesses;
    private final FilteredList<ProcessRecord> filteredProcesses;
    private final TextField searchField;
    private final Label footerStatusLabel;
    private final Button refreshMetricsButton;
    private final Button refreshProcessesButton;
    private final Button terminateProcessButton;
    private final Button navTerminateButton;
    private final Label displayModeLabel;
    private final Label statusBadgeLabel;
    private final BorderPane actionPane;
    private final HBox actionGroup;

    private Runnable onRefreshMetrics;
    private Runnable onRefreshProcesses;
    private Consumer<ProcessRecord> onTerminateProcess;
    
    private String lastProcessRefreshText = "en attente...";
    
    public DashboardView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");

        ramCard = new MetricCard("UTILISATION RAM");
        cpuCard = new MetricCard("PERFORMANCE CPU");
        systemCard = new VBox(16);
        systemCard.getStyleClass().addAll("metric-card", "system-card");
        systemCard.setPadding(new Insets(22));
        systemCard.setFillWidth(true);

        runtimeValueLabel = createSystemValueLabel();
        platformValueLabel = createSystemValueLabel();
        threadsValueLabel = createSystemValueLabel();
        uptimeValueLabel = createSystemValueLabel();

        runtimeValueLabel.setText(
                "Java " + System.getProperty("java.version") + " / JavaFX 21"
        );

        sourceProcesses = FXCollections.observableArrayList();
        filteredProcesses = new FilteredList<>(sourceProcesses, process -> true);
        processTable = new TableView<>();
        processTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        searchField = new TextField();
        footerStatusLabel = new Label("Dernière actualisation : en attente...");
        refreshMetricsButton = new Button("Actualiser RAM / CPU");
        refreshProcessesButton = new Button("Actualiser processus");
        terminateProcessButton = new Button("Arrêter le processus sélectionné");
        navTerminateButton = new Button("Arrêter");
        displayModeLabel = new Label();
        statusBadgeLabel = new Label("EN SURVEILLANCE");
        actionPane = new BorderPane();
        actionGroup = new HBox();

        buildLayout();
        configureResponsiveBehavior();
        bindEvents();
    }

    public Parent getRoot() {
        return root;
    }

    public void setOnRefreshMetrics(Runnable onRefreshMetrics) {
        this.onRefreshMetrics = onRefreshMetrics;
    }

    public void setOnRefreshProcesses(Runnable onRefreshProcesses) {
        this.onRefreshProcesses = onRefreshProcesses;
    }

    public void setOnTerminateProcess(Consumer<ProcessRecord> onTerminateProcess) {
        this.onTerminateProcess = onTerminateProcess;
    }

    public void updateMetrics(SystemMetrics metrics) {
        String usedMemory = Formatters.formatBytesToGigabytes(metrics.usedMemoryBytes());
        String totalMemory = Formatters.formatBytesToGigabytes(metrics.totalMemoryBytes());
        String freeMemory = Formatters.formatBytesToGigabytes(metrics.freeMemoryBytes());

        ramCard.update(
                usedMemory,
                "/ " + totalMemory,
                "",
                "",
                metrics.memoryUsageRatio(),
                Formatters.formatPercent(metrics.memoryUsageRatio()) + " Capacité",
                freeMemory + " libres"
        );

        cpuCard.update(
                Formatters.formatPercent(metrics.cpuUsageRatio()),
                "",
                "• live",
                "",
                metrics.cpuUsageRatio(),
                metrics.logicalProcessors() + " processeurs logiques",
                "Activité CPU (12 s)"
        );

        platformValueLabel.setText(
                metrics.operatingSystem() + " " + metrics.operatingSystemVersion() + " • " + metrics.architecture()
        );
        threadsValueLabel.setText(String.valueOf(Thread.getAllStackTraces().size()));
        uptimeValueLabel.setText(Formatters.formatUptime(metrics.uptimeSeconds()));
    }

//    public void updateProcesses(List<ProcessRecord> processes) {
//        sourceProcesses.setAll(processes);
//        footerStatusLabel.setText(
//                "Dernière actualisation : "
//                        + UI_DATE_TIME_FORMATTER.format(LocalDateTime.now())
//                        + "  •  " + processes.size() + " processus affichés"
//                        + "  • Actualisation planifiée : RAM/CPU (1s), Processus (4s)"
//        );
//    }
    
    public void updateProcesses(List<ProcessRecord> processes) {
        ProcessRecord currentSelection = processTable.getSelectionModel().getSelectedItem();
        Long selectedPid = currentSelection != null ? currentSelection.pid() : null;

        sourceProcesses.setAll(processes);
        restoreSelection(selectedPid);

        lastProcessRefreshText = UI_DATE_TIME_FORMATTER.format(LocalDateTime.now());
        updateFooterStatus();
    }
    
    
    private void restoreSelection(Long selectedPid) {
        if (selectedPid == null) {
            return;
        }

        ProcessRecord updatedSelection = filteredProcesses.stream()
                .filter(process -> process.pid() == selectedPid)
                .findFirst()
                .orElse(null);

        if (updatedSelection != null) {
            processTable.getSelectionModel().select(updatedSelection);
            processTable.scrollTo(updatedSelection);
        } else {
            processTable.getSelectionModel().clearSelection();
        }
    }
    
    public boolean confirmTermination(ProcessRecord processRecord) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation d'arrêt");
        alert.setHeaderText("Voulez-vous vraiment arrêter ce processus ?");
        alert.setContentText(
                "Processus : " + processRecord.name()
                        + "\nPID : " + processRecord.pid()
                        + "\nCommande : " + processRecord.command());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setTerminationInProgress(boolean inProgress) {
        terminateProcessButton.setDisable(inProgress);
        navTerminateButton.setDisable(inProgress);

        if (inProgress) {
            terminateProcessButton.setText("Arrêt en cours...");
            navTerminateButton.setText("Arrêt...");
        } else {
            terminateProcessButton.setText("Arrêter le processus sélectionné");
            navTerminateButton.setText("Arrêter");
        }
    }

    private void buildLayout() {
        root.setTop(buildAppBar());
        root.setCenter(buildScrollableCenter());
    }

    private HBox buildAppBar() {
        Label appName = new Label("MonitorFX");
        appName.getStyleClass().add("appbar-title");

        navTerminateButton.getStyleClass().addAll("nav-action-button", "nav-danger-button");
        navTerminateButton.setGraphic(new MaterialIcon(MaterialIcon.Type.BLOCK, 16));
        navTerminateButton.setContentDisplay(ContentDisplay.LEFT);

        HBox iconRow = new HBox(12,
                navTerminateButton,
                createTopIconChip(MaterialIcon.Type.DESKTOP_WINDOWS),
                createTopIconChip(MaterialIcon.Type.SETTINGS),
                createTopIconChip(MaterialIcon.Type.ACCOUNT_CIRCLE)
        );
        iconRow.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox appBar = new HBox(12, appName, spacer, iconRow);
        appBar.getStyleClass().add("appbar");
        appBar.setAlignment(Pos.CENTER_LEFT);
        return appBar;
    }

    private ScrollPane buildScrollableCenter() {
        FlowPane cardsFlow = buildCardsFlow();
        VBox processSection = buildProcessSection();

        VBox content = new VBox(22,
                buildHeroSection(),
                buildActionSection(),
                cardsFlow,
                processSection);
        content.getStyleClass().add("dashboard-content");
        content.setPadding(new Insets(28, 24, 24, 24));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("dashboard-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, bounds) ->
                cardsFlow.setPrefWrapLength(Math.max(bounds.getWidth() - 12, 320))
        );
        return scrollPane;
    }

    private BorderPane buildHeroSection() {
        Label title = new Label("Tableau de Bord");
        title.getStyleClass().add("dashboard-title");
        title.setWrapText(true);

        Label subtitle = new Label("Surveillance concurrente de la RAM, du CPU et des processus");
        subtitle.getStyleClass().add("dashboard-subtitle");
        subtitle.setWrapText(true);

        VBox titleBox = new VBox(8, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        StackPane badgeDot = new StackPane();
        badgeDot.getStyleClass().add("status-dot");
        statusBadgeLabel.getStyleClass().add("status-badge-label");

        HBox badge = new HBox(10, badgeDot, statusBadgeLabel);
        badge.getStyleClass().add("status-badge");
        badge.setAlignment(Pos.CENTER);

        BorderPane hero = new BorderPane();
        hero.setLeft(titleBox);
        hero.setRight(badge);
        BorderPane.setAlignment(badge, Pos.CENTER_RIGHT);
        return hero;
    }

    private BorderPane buildActionSection() {
        refreshMetricsButton.getStyleClass().addAll("action-button", "primary-button");
        refreshProcessesButton.getStyleClass().addAll("action-button", "secondary-button");
        terminateProcessButton.getStyleClass().addAll("action-button", "danger-button");

        refreshMetricsButton.setGraphic(new MaterialIcon(MaterialIcon.Type.REFRESH, 18));
        refreshProcessesButton.setGraphic(new MaterialIcon(MaterialIcon.Type.SYNC_ALT, 18));
        terminateProcessButton.setGraphic(new MaterialIcon(MaterialIcon.Type.BLOCK, 18));

        refreshMetricsButton.setContentDisplay(ContentDisplay.LEFT);
        refreshProcessesButton.setContentDisplay(ContentDisplay.LEFT);
        terminateProcessButton.setContentDisplay(ContentDisplay.LEFT);

        actionGroup.getStyleClass().add("action-group");
        actionGroup.setSpacing(12);
        actionGroup.setAlignment(Pos.CENTER_LEFT);
        actionGroup.getChildren().setAll(refreshMetricsButton, refreshProcessesButton);

        actionPane.getStyleClass().add("action-pane");
        actionPane.setMaxWidth(Double.MAX_VALUE);
        actionPane.setLeft(actionGroup);
        actionPane.setRight(terminateProcessButton);
        BorderPane.setAlignment(terminateProcessButton, Pos.CENTER_RIGHT);
        return actionPane;
    }

    private FlowPane buildCardsFlow() {
        ramCard.setHeaderGraphic(createCardIcon(MaterialIcon.Type.MEMORY, "icon-blue"));
        cpuCard.setHeaderGraphic(createCardIcon(MaterialIcon.Type.MONITORING, "icon-green"));
        cpuCard.setSparkMode(true);
        buildSystemCardContent();

        FlowPane cardsFlow = new FlowPane();
        cardsFlow.getStyleClass().add("cards-flow");
        cardsFlow.setHgap(18);
        cardsFlow.setVgap(18);
        cardsFlow.getChildren().addAll(ramCard, cpuCard, systemCard);
        return cardsFlow;
    }

    private void buildSystemCardContent() {
        HBox header = createCardHeader("INFO SYSTÈME", createSectionIcon(MaterialIcon.Type.INFO, "icon-muted"));

        VBox rows = new VBox(12,
                createInfoRow("Runtime", runtimeValueLabel),
                createInfoRow("Plateforme", platformValueLabel),
                createInfoRow("Threads Actifs", threadsValueLabel),
                createInfoRow("Uptime", uptimeValueLabel));

        systemCard.getChildren().setAll(header, rows);
    }

    private VBox buildProcessSection() {
        HBox titleRow = new HBox(12,
                createSectionIcon(MaterialIcon.Type.LIST_ALT, "icon-blue"),
                createSectionTitleLabel("Applications / Processus en cours d'exécution")
        );
        titleRow.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Rechercher un processus par nom ou PID...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(420);
        searchField.setMaxWidth(Double.MAX_VALUE);

        HBox searchBox = new HBox(10,
                createSearchAdornment(),
                searchField
        );
        searchBox.getStyleClass().add("search-shell");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        BorderPane sectionHeader = new BorderPane();
        sectionHeader.setLeft(titleRow);
        sectionHeader.setRight(searchBox);
        BorderPane.setMargin(searchBox, new Insets(0, 0, 0, 18));

        Label searchHint = new Label("Tri principal : temps CPU décroissant | Affichage complet des processus détectés");
        searchHint.getStyleClass().add("section-hint");
        searchHint.setWrapText(true);

        displayModeLabel.getStyleClass().add("section-hint");
        displayModeLabel.setWrapText(true);

        footerStatusLabel.getStyleClass().add("footer-label");
        footerStatusLabel.setWrapText(true);
        footerStatusLabel.setTextAlignment(TextAlignment.LEFT);

        configureProcessTable();

        VBox section = new VBox(16, sectionHeader, searchHint, displayModeLabel, processTable, footerStatusLabel);
        section.getStyleClass().add("section-card");
        VBox.setVgrow(processTable, Priority.ALWAYS);
        processTable.setPrefHeight(540);
        return section;
    }

    private void bindEvents() {
        refreshMetricsButton.setOnAction(event -> {
            if (onRefreshMetrics != null) {
                onRefreshMetrics.run();
            }
        });

        refreshProcessesButton.setOnAction(event -> {
            if (onRefreshProcesses != null) {
                onRefreshProcesses.run();
            }
        });

        terminateProcessButton.setOnAction(event -> terminateSelectedProcess());

        navTerminateButton.setOnAction(event -> terminateSelectedProcess());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String filter = newValue == null ? "" : newValue.toLowerCase(Locale.ROOT).trim();
            filteredProcesses.setPredicate(process -> {
                if (filter.isBlank()) {
                    return true;
                }
                return String.valueOf(process.pid()).contains(filter)
                        || safeLower(process.name()).contains(filter)
                        || safeLower(process.user()).contains(filter)
                        || safeLower(process.command()).contains(filter)
                        || safeLower(process.startTime()).contains(filter)
                        || safeLower(process.cpuDuration()).contains(filter);
            });

            updateFooterStatus();
        });
    }
    
    
    private void updateFooterStatus() {
        int totalProcesses = sourceProcesses.size();
        int visibleProcesses = filteredProcesses.size();

        String currentFilter = searchField.getText() == null
                ? ""
                : searchField.getText().trim();

        String processInfo;
        if (currentFilter.isBlank()) {
            processInfo = visibleProcesses + " processus affichés";
        } else {
            processInfo = visibleProcesses + " / " + totalProcesses
                    + " processus affichés"
                    + "  •  Recherche : \"" + currentFilter + "\"";
        }

        footerStatusLabel.setText(
                "Dernière actualisation : " + lastProcessRefreshText
                        + "  •  " + processInfo
                        + "  • Actualisation planifiée : RAM/CPU (1s), Processus (4s)"
        );
    }

    private void terminateSelectedProcess() {
        ProcessRecord selected = processTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un processus dans le tableau.");
            return;
        }
        if (onTerminateProcess != null) {
            onTerminateProcess.accept(selected);
        }
    }

    private void configureResponsiveBehavior() {
        root.widthProperty().addListener((observable, oldValue, newValue) ->
                updateResponsiveState(newValue.doubleValue())
        );
        updateResponsiveState(1280);
    }

    private void updateResponsiveState(double width) {
        actionPane.setBottom(null);
        actionPane.setRight(terminateProcessButton);
        BorderPane.setMargin(terminateProcessButton, Insets.EMPTY);
        BorderPane.setAlignment(terminateProcessButton, Pos.CENTER_RIGHT);

        if (width < 1180) {
            processTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            displayModeLabel.setText("Mode adaptatif : écran compact détecté, défilement horizontal activé pour préserver la lisibilité.");
        } else {
            processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            displayModeLabel.setText("Mode adaptatif : écran large détecté, le tableau exploite toute la largeur disponible.");
        }
    }

    private void configureProcessTable() {
        processTable.setItems(filteredProcesses);
        processTable.getStyleClass().add("process-table");
        processTable.setPlaceholder(new Label("Aucun processus ne correspond à la recherche."));
        processTable.setMinHeight(320);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        processTable.setFixedCellSize(58);
        processTable.setTableMenuButtonVisible(false);

        TableColumn<ProcessRecord, Long> pidColumn = new TableColumn<>("PID");
        pidColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().pid()));
        pidColumn.setPrefWidth(100);
        pidColumn.setMinWidth(90);

        TableColumn<ProcessRecord, String> nameColumn = new TableColumn<>("PROCESSUS");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        nameColumn.setPrefWidth(220);
        nameColumn.setMinWidth(180);

        TableColumn<ProcessRecord, String> userColumn = new TableColumn<>("UTILISATEUR");
        userColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().user()));
        userColumn.setPrefWidth(220);
        userColumn.setMinWidth(170);

        TableColumn<ProcessRecord, String> startColumn = new TableColumn<>("DÉMARRAGE");
        startColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().startTime()));
        startColumn.setPrefWidth(170);
        startColumn.setMinWidth(150);

        TableColumn<ProcessRecord, String> cpuColumn = new TableColumn<>("TEMPS CPU");
        cpuColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cpuDuration()));
        cpuColumn.setPrefWidth(130);
        cpuColumn.setMinWidth(120);
        cpuColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("cpu-cell");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("cpu-cell")) {
                        getStyleClass().add("cpu-cell");
                    }
                }
            }
        });

        TableColumn<ProcessRecord, Boolean> stateColumn = new TableColumn<>("ÉTAT");
        stateColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().alive()));
        stateColumn.setPrefWidth(130);
        stateColumn.setMinWidth(120);
        stateColumn.setCellFactory(createStateCellFactory());

        TableColumn<ProcessRecord, String> commandColumn = new TableColumn<>("COMMANDE");
        commandColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().command()));
        commandColumn.setPrefWidth(360);
        commandColumn.setMinWidth(240);

        processTable.setRowFactory(table -> new TableRowHighlight());
        processTable.getColumns().setAll(
                pidColumn,
                nameColumn,
                userColumn,
                startColumn,
                cpuColumn,
                stateColumn,
                commandColumn
        );
    }

    private Callback<TableColumn<ProcessRecord, Boolean>, TableCell<ProcessRecord, Boolean>> createStateCellFactory() {
        return column -> new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.setContentDisplay(ContentDisplay.TEXT_ONLY);
                badge.getStyleClass().add("state-pill");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Boolean alive, boolean empty) {
                super.updateItem(alive, empty);
                if (empty || alive == null) {
                    setGraphic(null);
                    return;
                }
                badge.getStyleClass().removeAll("state-running", "state-stopped");
                if (alive) {
                    badge.setText("EXÉCUTION");
                    badge.getStyleClass().add("state-running");
                } else {
                    badge.setText("TERMINÉ");
                    badge.getStyleClass().add("state-stopped");
                }
                setGraphic(badge);
            }
        };
    }

    private HBox createCardHeader(String title, StackPane icon) {
        Label label = new Label(title);
        label.getStyleClass().add("metric-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, label, spacer, icon);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private StackPane createTopIconChip(MaterialIcon.Type type) {
        StackPane chip = new StackPane(new MaterialIcon(type, 18));
        chip.getStyleClass().addAll("top-icon-chip", "icon-muted");
        return chip;
    }

    private StackPane createCardIcon(MaterialIcon.Type type, String iconVariantClass) {
        MaterialIcon icon = new MaterialIcon(type, 18);
        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add(iconVariantClass);
        return shell;
    }

    private StackPane createSectionIcon(MaterialIcon.Type type, String iconVariantClass) {
        MaterialIcon icon = new MaterialIcon(type, 20);
        StackPane shell = new StackPane(icon);
        shell.getStyleClass().addAll("section-icon-shell", iconVariantClass);
        return shell;
    }

    private StackPane createSearchAdornment() {
        StackPane shell = new StackPane(new MaterialIcon(MaterialIcon.Type.SEARCH, 18));
        shell.getStyleClass().addAll("search-icon-shell", "icon-muted");
        return shell;
    }

    private Label createSectionTitleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        label.setWrapText(true);
        return label;
    }

    private Label createSystemValueLabel() {
        Label label = new Label("--");
        label.getStyleClass().add("system-value");
        label.setWrapText(true);
        return label;
    }

    private HBox createInfoRow(String key, Label valueLabel) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("system-key");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, keyLabel, spacer, valueLabel);
        row.getStyleClass().add("system-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class TableRowHighlight extends javafx.scene.control.TableRow<ProcessRecord> {
        @Override
        protected void updateItem(ProcessRecord item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("row-terminated");
            if (!empty && item != null && !item.alive()) {
                getStyleClass().add("row-terminated");
            }
        }
    }
}
