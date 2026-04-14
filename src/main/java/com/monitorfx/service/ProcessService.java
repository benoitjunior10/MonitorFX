package com.monitorfx.service;

import com.monitorfx.model.ProcessRecord;
import com.monitorfx.util.Formatters;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service chargé de collecter et de piloter les processus système.
 */
public class ProcessService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRENCH)
                    .withZone(ZoneId.systemDefault());
    private static final Duration GRACEFUL_TERMINATION_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration FORCED_TERMINATION_TIMEOUT = Duration.ofSeconds(3);

    public record TerminationResult(boolean success, String title, String detail) {
        public String message(ProcessRecord processRecord) {
            return "Processus : " + processRecord.name()
                    + " (PID " + processRecord.pid() + ")"
                    + "\n" + detail;
        }
    }

    public List<ProcessRecord> captureProcesses() {
        return ProcessHandle.allProcesses()
                .map(this::toProcessRecord)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(ProcessRecord::cpuDurationMillis).reversed())
                .toList();
    }

    public TerminationResult terminateProcess(long pid) {
        if (pid == ProcessHandle.current().pid()) {
            return new TerminationResult(
                    false,
                    "Arrêt refusé",
                    "L'application ne peut pas tenter d'arrêter son propre processus.");
        }

        Optional<ProcessHandle> optionalHandle = ProcessHandle.of(pid);
        if (optionalHandle.isEmpty()) {
            return new TerminationResult(
                    false,
                    "Processus introuvable",
                    "Le processus n'existe plus ou a déjà quitté avant la tentative d'arrêt.");
        }

        ProcessHandle handle = optionalHandle.get();
        if (!handle.isAlive()) {
            return new TerminationResult(
                    true,
                    "Processus déjà terminé",
                    "Le processus n'était déjà plus actif au moment de la vérification.");
        }

        try {
            boolean normalTerminationSupported = handle.supportsNormalTermination();

            if (normalTerminationSupported) {
                handle.destroy();
                if (waitForExit(handle, GRACEFUL_TERMINATION_TIMEOUT)) {
                    return new TerminationResult(
                            true,
                            "Processus arrêté",
                            "Le processus a été arrêté correctement après une demande d'arrêt normale.");
                }

                handle.destroyForcibly();
                if (waitForExit(handle, FORCED_TERMINATION_TIMEOUT)) {
                    return new TerminationResult(
                            true,
                            "Processus arrêté de force",
                            "Le processus n'a pas répondu à l'arrêt normal dans le délai prévu et a été terminé de force.");
                }
            } else {
                handle.destroyForcibly();
                if (waitForExit(handle, FORCED_TERMINATION_TIMEOUT)) {
                    return new TerminationResult(
                            true,
                            "Processus arrêté",
                            "Le processus a été arrêté. La plateforme ne permet pas ici de distinguer un arrêt normal d'un arrêt forcé.");
                }
            }

            return new TerminationResult(
                    false,
                    "Arrêt impossible",
                    "Le processus n'a pas quitté dans le délai attendu après les tentatives d'arrêt normal et forcé. Des privilèges supplémentaires peuvent être nécessaires.");

        } catch (SecurityException exception) {
            return new TerminationResult(
                    false,
                    "Accès refusé",
                    "L'application ne dispose pas des permissions requises pour arrêter ce processus.");

        } catch (RuntimeException exception) {
            return new TerminationResult(
                    false,
                    "Erreur d'arrêt",
                    "Une erreur interne est survenue pendant la tentative d'arrêt : " + safeMessage(exception));
        }
    }

    private boolean waitForExit(ProcessHandle handle, Duration timeout) {
        try {
            handle.onExit().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return !handle.isAlive();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return !handle.isAlive();

        } catch (TimeoutException | ExecutionException exception) {
            return !handle.isAlive();
        }
    }

    private ProcessRecord toProcessRecord(ProcessHandle processHandle) {
        try {
            ProcessHandle.Info info = processHandle.info();

            String command = info.command().orElse("Processus système");
            String commandLine = info.commandLine().orElse(command);
            String processName = extractProcessName(command);
            String user = info.user().orElse("N/A");
            Instant startInstant = info.startInstant().orElse(null);
            long cpuMillis = info.totalCpuDuration().map(duration -> duration.toMillis()).orElse(0L);
            String cpuDuration = info.totalCpuDuration()
                    .map(Formatters::formatDuration)
                    .orElse("00:00:00");
            String startTime = startInstant == null ? "--" : DATE_TIME_FORMATTER.format(startInstant);

            return new ProcessRecord(
                    processHandle.pid(),
                    processName,
                    user,
                    startTime,
                    cpuDuration,
                    commandLine,
                    processHandle.isAlive(),
                    cpuMillis
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractProcessName(String command) {
        if (command == null || command.isBlank()) {
            return "Inconnu";
        }

        String normalized = command.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
