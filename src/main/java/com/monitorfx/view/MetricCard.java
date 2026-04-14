/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.view;

/**
 *
 * @author HP
 * Carte KPI professionnelle pour le dashboard.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class MetricCard extends VBox {

    private static final int DEFAULT_SPARK_POINT_CAPACITY = 12;
    private static final double SPARK_BAR_SPACING = 8.0;
    private static final double SPARK_MIN_HEIGHT = 10.0;
    private static final double SPARK_MAX_HEIGHT = 44.0;
    private static final double SPARK_MIN_WIDTH = 8.0;
    private static final double SPARK_ARC = 8.0;

    private final Label titleLabel;
    private final StackPane iconSlot;
    private final Label valueLabel;
    private final Label suffixLabel;
    private final Label trendLabel;
    private final Label subtitleLabel;
    private final ProgressBar progressBar;
    private final HBox sparkBarsBox;
    private final List<Rectangle> sparkBars;
    private final Deque<Double> sparkHistory;
    private final int sparkPointCapacity;
    private final Label footerLeftLabel;
    private final Label footerRightLabel;

    private boolean sparkMode;

    public MetricCard(String title) {
        this(title, DEFAULT_SPARK_POINT_CAPACITY);
    }

    public MetricCard(String title, int sparkPointCapacity) {
        this.sparkPointCapacity = Math.max(4, sparkPointCapacity);
        titleLabel = new Label(title);
        iconSlot = new StackPane();
        valueLabel = new Label("--");
        suffixLabel = new Label();
        trendLabel = new Label();
        subtitleLabel = new Label();
        progressBar = new ProgressBar(0.0);
        sparkBarsBox = new HBox(SPARK_BAR_SPACING);
        sparkBars = new ArrayList<>();
        sparkHistory = new ArrayDeque<>(this.sparkPointCapacity);
        footerLeftLabel = new Label();
        footerRightLabel = new Label();

        getStyleClass().add("metric-card");
        setSpacing(16);
        setPadding(new Insets(22));
        setFillWidth(true);

        titleLabel.getStyleClass().add("metric-title");
        iconSlot.getStyleClass().add("card-icon-shell");
        iconSlot.setMinSize(32, 32);
        iconSlot.setPrefSize(32, 32);
        iconSlot.setMaxSize(32, 32);

        valueLabel.getStyleClass().add("metric-value");
        suffixLabel.getStyleClass().add("metric-suffix");
        trendLabel.getStyleClass().add("metric-trend");
        subtitleLabel.getStyleClass().add("metric-details");
        subtitleLabel.setWrapText(true);

        progressBar.getStyleClass().add("metric-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        sparkBarsBox.getStyleClass().add("metric-sparkbars");
        sparkBarsBox.setAlignment(Pos.BOTTOM_LEFT);
        sparkBarsBox.setMinHeight(SPARK_MAX_HEIGHT + 4);
        sparkBarsBox.setPrefHeight(SPARK_MAX_HEIGHT + 4);
        sparkBarsBox.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < this.sparkPointCapacity; i++) {
            Rectangle bar = new Rectangle();
            bar.setHeight(SPARK_MIN_HEIGHT);
            bar.setArcWidth(SPARK_ARC);
            bar.setArcHeight(SPARK_ARC);
            bar.getStyleClass().add("metric-sparkbar");
            bar.widthProperty().bind(Bindings.createDoubleBinding(
                    () -> {
                        double totalSpacing = Math.max(0.0, (this.sparkPointCapacity - 1) * sparkBarsBox.getSpacing());
                        double availableWidth = Math.max(0.0, sparkBarsBox.getWidth() - totalSpacing);
                        return Math.max(SPARK_MIN_WIDTH, availableWidth / this.sparkPointCapacity);
                    },
                    sparkBarsBox.widthProperty(),
                    sparkBarsBox.spacingProperty()
            ));
            sparkBars.add(bar);
            sparkBarsBox.getChildren().add(bar);
        }

        footerLeftLabel.getStyleClass().add("metric-footer-left");
        footerRightLabel.getStyleClass().add("metric-footer-right");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleLabel, headerSpacer, iconSlot);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox valueRow = new HBox(8, valueLabel, suffixLabel, trendLabel);
        valueRow.setAlignment(Pos.BASELINE_LEFT);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, footerLeftLabel, footerSpacer, footerRightLabel);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, valueRow, subtitleLabel, progressBar, sparkBarsBox, footer);
        setSparkMode(false);
        setHeaderGraphic(null);
        setSubtitle("");
    }

    public void setHeaderGraphic(Node icon) {
        if (icon == null) {
            iconSlot.getChildren().clear();
            iconSlot.setManaged(false);
            iconSlot.setVisible(false);
            return;
        }
        iconSlot.getChildren().setAll(icon);
        iconSlot.setManaged(true);
        iconSlot.setVisible(true);
    }

    public void setSparkMode(boolean sparkMode) {
        this.sparkMode = sparkMode;
        progressBar.setManaged(!sparkMode);
        progressBar.setVisible(!sparkMode);
        sparkBarsBox.setManaged(sparkMode);
        sparkBarsBox.setVisible(sparkMode);
    }

    public void setSubtitle(String text) {
        boolean visible = text != null && !text.isBlank();
        subtitleLabel.setText(visible ? text : "");
        subtitleLabel.setManaged(visible);
        subtitleLabel.setVisible(visible);
    }

    public void update(String value, String suffix, String trend, String subtitle,
                       double progress, String footerLeft, String footerRight) {
        valueLabel.setText(value == null ? "--" : value);

        boolean suffixVisible = suffix != null && !suffix.isBlank();
        suffixLabel.setText(suffixVisible ? suffix : "");
        suffixLabel.setManaged(suffixVisible);
        suffixLabel.setVisible(suffixVisible);

        boolean trendVisible = trend != null && !trend.isBlank();
        trendLabel.setText(trendVisible ? trend : "");
        trendLabel.setManaged(trendVisible);
        trendLabel.setVisible(trendVisible);

        setSubtitle(subtitle);

        double safeProgress = Math.max(0.0, Math.min(1.0, progress));
        if (sparkMode) {
            pushSparkValue(safeProgress);
            updateSparkBars();
        } else {
            progressBar.setProgress(safeProgress);
        }

        footerLeftLabel.setText(footerLeft == null ? "" : footerLeft);
        footerRightLabel.setText(footerRight == null ? "" : footerRight);
    }

    private void pushSparkValue(double value) {
        if (sparkHistory.size() == sparkPointCapacity) {
            sparkHistory.removeFirst();
        }
        sparkHistory.addLast(value);
    }

    private void updateSparkBars() {
        List<Double> values = new ArrayList<>(sparkHistory);
        while (values.size() < sparkPointCapacity) {
            values.add(0, 0.0);
        }

        int latestIndex = values.size() - 1;
        for (int i = 0; i < sparkBars.size(); i++) {
            Rectangle bar = sparkBars.get(i);
            double normalizedValue = values.get(i);
            double height = SPARK_MIN_HEIGHT + (normalizedValue * (SPARK_MAX_HEIGHT - SPARK_MIN_HEIGHT));
            bar.setHeight(height);

            bar.getStyleClass().removeAll("metric-sparkbar-accent", "metric-sparkbar-highlight");
            if (i == latestIndex) {
                bar.getStyleClass().add("metric-sparkbar-highlight");
            } else if (i >= Math.max(0, latestIndex - 2)) {
                bar.getStyleClass().add("metric-sparkbar-accent");
            }
        }
    }
}
