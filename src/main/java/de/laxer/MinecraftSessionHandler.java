package de.laxer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

public class MinecraftSessionHandler {

    private final ExecutorService executorService;
    private final Logger logger;

    public MinecraftSessionHandler(Logger logger, ExecutorService executorService) {
        this.logger = logger;
        this.executorService = executorService;
    }

    /**
     * Prüft asynchron, ob der Serverprozess läuft (via pgrep).
     * Gibt true zurück, wenn der Prozess gefunden wird, sonst false.
     * Wirft eine Exception bei Fehlern während der Ausführung.
     */
    public CompletableFuture<Boolean> checkServerStatusAsync() {
        return CompletableFuture.supplyAsync(() -> {
            // Wichtig: Sicherstellen, dass 'server_start_script_name' eindeutig genug ist!
            // Ein vollständiger Pfad ist oft besser.
            // Beispiel: pgrep -f "/home/user/mcserver/start2.sh"
            String commandToFind = Config.server_start_script_name;
            String[] cmd = { "/bin/bash", "-c", "pgrep -a -f \"" + commandToFind + "\"" };
            logger.debug("Führe Statusprüfung aus: {}", String.join(" ", cmd));
            try {
                ProcessResult result = executeCommand(cmd, 5); // 5 Sekunden Timeout
                logger.debug("pgrep Exit Code: {}", result.exitCode());
                logger.debug("pgrep stdout: {}", result.stdout().isEmpty() ? "<leer>" : result.stdout());
                logger.debug("pgrep stderr: {}", result.stderr().isEmpty() ? "<leer>" : result.stderr());

                // Exit Code 0: Prozess(e) gefunden -> Online
                // Exit Code 1: Kein Prozess gefunden -> Offline
                // Andere Codes: Fehler bei pgrep
                if (result.exitCode() == 0) {
                    return true;
                } else if (result.exitCode() == 1) {
                    return false;
                } else {
                    // Unerwarteter Exit-Code von pgrep
                    logger.warn("Unerwarteter Exit-Code von pgrep: {}, stderr: {}", result.exitCode(), result.stderr());
                    throw new RuntimeException("Fehler bei pgrep Ausführung (Exit Code: " + result.exitCode() + ")");
                }
            } catch (Exception e) {
                // Fange Timeouts oder andere Ausführungsfehler ab
                logger.error("Fehler beim Ausführen von pgrep: {}", e.getMessage(), e);
                throw new RuntimeException("Fehler bei der Serverstatusprüfung via pgrep", e);
            }
        }, executorService); // Führe dies im ExecutorService aus
    }

    /**
     * Sendet asynchron den Startbefehl an die tmux-Session.
     * Gibt true zurück, wenn der Befehl erfolgreich gesendet wurde (Exit Code 0),
     * sonst false.
     * Wirft eine Exception bei Fehlern während der Ausführung.
     */
    public CompletableFuture<Boolean> startMcServerAsync() {
        return CompletableFuture.supplyAsync(() -> {
            // Stelle sicher, dass der Pfad zum Startskript korrekt ist! Absoluter Pfad
            // empfohlen.
            String commandToSend = "bash " + Config.server_start_script_name; // z.B. bash /home/user/mc/start.sh
            // Der Befehl, der an tmux gesendet wird. 'C-m' simuliert Enter.
            String tmuxCommand = String.format("unset TMUX; tmux send-keys -t %s '%s' C-m",
                    Config.tmux_session_name, commandToSend);
            String[] cmd = { "/bin/bash", "-c", tmuxCommand };
            logger.info("Sende Startbefehl an tmux: {}", tmuxCommand);
            try {
                // Prüfe zuerst, ob die tmux Session existiert (optional, aber gut)
                String[] checkSessionCmd = { "/bin/bash", "-c", "tmux has-session -t " + Config.tmux_session_name };
                ProcessResult sessionCheck = executeCommand(checkSessionCmd, 3);
                if (sessionCheck.exitCode() != 0) {
                    logger.error("tmux Session '{}' nicht gefunden! Server kann nicht gestartet werden. stderr: {}",
                            Config.tmux_session_name, sessionCheck.stderr());
                    throw new RuntimeException("tmux Session '" + Config.tmux_session_name + "' existiert nicht.");
                }
                logger.debug("tmux Session '{}' gefunden.", Config.tmux_session_name);

                // Session existiert, sende den Startbefehl
                ProcessResult result = executeCommand(cmd, 10); // 10 Sekunden Timeout
                logger.info("tmux send-keys Exit Code: {}", result.exitCode());
                logger.debug("tmux send-keys stdout: {}", result.stdout().isEmpty() ? "<leer>" : result.stdout());
                logger.debug("tmux send-keys stderr: {}", result.stderr().isEmpty() ? "<leer>" : result.stderr());

                if (result.exitCode() == 0) {
                    logger.info("Startbefehl erfolgreich an tmux-Session '{}' gesendet.", Config.tmux_session_name);
                    return true;
                } else {
                    logger.error("Fehler beim Senden des Startbefehls an tmux (Exit Code {}). stderr: {}",
                            result.exitCode(), result.stderr());
                    throw new RuntimeException("tmux send-keys fehlgeschlagen (Exit Code: " + result.exitCode() + ")");
                }
            } catch (Exception e) {
                logger.error("Fehler beim Ausführen von tmux Befehl: {}", e.getMessage(), e);
                // Weiterwerfen, damit exceptionally greift
                throw new RuntimeException("Fehler beim Senden des Startbefehls an tmux", e);
            }
        }, executorService); // Führe dies im ExecutorService aus
    }

    /**
     * Hilfsmethode zum Ausführen externer Befehle mit Timeout und Stream-Handling.
     * Liest stdout und stderr asynchron, um Deadlocks zu vermeiden.
     */
    public ProcessResult executeCommand(String[] command, long timeoutSeconds) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // Verwende CompletableFuture, um Streams parallel zu lesen
        CompletableFuture<String> stdoutFuture = CompletableFuture
                .supplyAsync(() -> readStream(process.getInputStream()), executorService);
        CompletableFuture<String> stderrFuture = CompletableFuture
                .supplyAsync(() -> readStream(process.getErrorStream()), executorService);

        boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finishedInTime) {
            process.destroyForcibly(); // Prozess beenden
            // Warte kurz auf die Reader-Futures, aber nicht ewig
            stdoutFuture.completeExceptionally(new RuntimeException("Process timed out"));
            stderrFuture.completeExceptionally(new RuntimeException("Process timed out"));
            throw new RuntimeException(
                    "Timeout (" + timeoutSeconds + "s) beim Warten auf Befehl: " + String.join(" ", command));
        }

        // Warte auf Abschluss der Stream-Reader und hole Ergebnisse
        String stdout = stdoutFuture.get(1, TimeUnit.SECONDS); // Kurzer Timeout für get
        String stderr = stderrFuture.get(1, TimeUnit.SECONDS); // Kurzer Timeout für get

        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    /** Liest einen InputStream vollständig und gibt ihn als String zurück. */
    public String readStream(java.io.InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.warn("Fehler beim Lesen eines Prozess-Streams: {}", e.getMessage());
            return "Fehler beim Lesen des Streams: " + e.getMessage();
        }
    }
}
