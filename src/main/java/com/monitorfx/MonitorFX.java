/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.monitorfx;

/**
 *
 * @author HP
 */

import com.monitorfx.controller.DashboardController;
import com.monitorfx.view.DashboardView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application.
 * Demarre la vue et le contrôleur principal.
 */
public class MonitorFX extends Application {

    private DashboardController coordinator;

    @Override
    public void start(Stage stage) {
        DashboardView dashboardView = new DashboardView();
        coordinator = new DashboardController(dashboardView);
        coordinator.start();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double initialWidth = clamp(screenBounds.getWidth() * 0.92, 980, 1440);
        double initialHeight = clamp(screenBounds.getHeight() * 0.90, 680, 920);

        Scene scene = new Scene(dashboardView.getRoot(), initialWidth, initialHeight);
        String css = getClass().getResource("/dashboard.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("MonitorFX - Plateforme de surveillance systeme");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);

        if (screenBounds.getWidth() <= 1366 || screenBounds.getHeight() <= 768) {
            stage.setMaximized(true);
        }

        stage.show();
        stage.setOnCloseRequest(event -> coordinator.shutdown());
    }

    @Override
    public void stop() {
        if (coordinator != null) {
            coordinator.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

