package com.monitorfx.controller;

import com.monitorfx.model.ProcessRecord;
import com.monitorfx.model.SystemMetrics;
import com.monitorfx.service.ProcessService;
import com.monitorfx.service.SystemMetricsService;
import com.monitorfx.util.RefreshScheduler;
import com.monitorfx.view.DashboardView;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contrôleur principal MVC.
 * Relie la vue JavaFX aux services métier et à la planification concurrente.
 */
public class DashboardController {

    private final DashboardView dashboardView;
    private final SystemMetricsService systemMetricsService;
    private final ProcessService processService;
    private final RefreshScheduler scheduler;
    private final AtomicBoolean metricsRefreshInProgress;
    private final AtomicBoolean processesRefreshInProgress;
    private final AtomicBoolean terminationInProgress;

    public DashboardController(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.systemMetricsService = new SystemMetricsService();
        this.processService = new ProcessService();
        this.scheduler = new RefreshScheduler(3);
        this.metricsRefreshInProgress = new AtomicBoolean(false);
        this.processesRefreshInProgress = new AtomicBoolean(false);
        this.terminationInProgress = new AtomicBoolean(false);
        bindActions();
    }

    public void start() {
        refreshMetricsAsync();
        refreshProcessesAsync();

        scheduler.scheduleAtFixedRate(this::refreshMetricsSafely, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::refreshProcessesSafely, 4, 4, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private void bindActions() {
        dashboardView.setOnRefreshMetrics(this::refreshMetricsAsync);
        dashboardView.setOnRefreshProcesses(this::refreshProcessesAsync);
        dashboardView.setOnTerminateProcess(this::terminateProcess);
    }

    private void refreshMetricsAsync() {
        scheduler.execute(this::refreshMetricsSafely);
    }

    private void refreshProcessesAsync() {
        scheduler.execute(this::refreshProcessesSafely);
    }

    private void refreshMetricsSafely() {
        if (!metricsRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            SystemMetrics metrics = systemMetricsService.captureSnapshot();
            Platform.runLater(() -> dashboardView.updateMetrics(metrics));
        } catch (Exception exception) {
            Platform.runLater(() -> dashboardView.showError(
                    "Erreur de surveillance",
                    "Impossible de récupérer les métriques système : " + safeMessage(exception)));
        } finally {
            metricsRefreshInProgress.set(false);
        }
    }

    private void refreshProcessesSafely() {
        if (!processesRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            List<ProcessRecord> processes = processService.captureProcesses();
            Platform.runLater(() -> dashboardView.updateProcesses(processes));
        } catch (Exception exception) {
            Platform.runLater(() -> dashboardView.showError(
                    "Erreur processus",
                    "Impossible de récupérer la liste des processus : " + safeMessage(exception)));
        } finally {
            processesRefreshInProgress.set(false);
        }
    }

    private void terminateProcess(ProcessRecord processRecord) {
        if (!terminationInProgress.compareAndSet(false, true)) {
            dashboardView.showError(
                    "Arrêt déjà en cours",
                    "Veuillez attendre la fin de la tentative d'arrêt actuelle avant d'en lancer une nouvelle.");
            return;
        }

        boolean confirmed = false;
        try {
            confirmed = dashboardView.confirmTermination(processRecord);
            if (!confirmed) {
                return;
            }

            dashboardView.setTerminationInProgress(true);
            scheduler.execute(() -> executeTermination(processRecord));

        } catch (RejectedExecutionException exception) {
            terminationInProgress.set(false);
            dashboardView.setTerminationInProgress(false);
            dashboardView.showError(
                    "Arrêt impossible",
                    "Le service d'exécution est indisponible. L'application est peut-être en cours de fermeture.");

        } catch (Exception exception) {
            terminationInProgress.set(false);
            dashboardView.setTerminationInProgress(false);
            dashboardView.showError(
                    "Erreur d'arrêt",
                    "Une erreur est survenue lors de la préparation de l'arrêt du processus : " + safeMessage(exception));

        } finally {
            if (!confirmed) {
                terminationInProgress.set(false);
                dashboardView.setTerminationInProgress(false);
            }
        }
    }

    private void executeTermination(ProcessRecord processRecord) {
        try {
            ProcessService.TerminationResult result = processService.terminateProcess(processRecord.pid());

            Platform.runLater(() -> {
                if (result.success()) {
                    dashboardView.showInfo(result.title(), result.message(processRecord));
                } else {
                    dashboardView.showError(result.title(), result.message(processRecord));
                }
                refreshProcessesAsync();
            });

        } catch (Exception exception) {
            Platform.runLater(() -> dashboardView.showError(
                    "Erreur d'arrêt",
                    "Une erreur est survenue lors de l'arrêt du processus : " + safeMessage(exception)));

        } finally {
            terminationInProgress.set(false);
            Platform.runLater(() -> dashboardView.setTerminationInProgress(false));
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
