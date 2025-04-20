package de.laxer;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MessageReact extends ListenerAdapter {

    // SLF4j Logger
    private static final Logger logger = LoggerFactory.getLogger(MessageReact.class);


    // Konfiguration aus DiscordBotMain holen
    private final String prefix = DiscordBotMain.getPrefix();
    private final String server_ip = DiscordBotMain.getServer_ip();
    private final String server_start_script_name = DiscordBotMain.getServerStartingScriptName();
    private final String tmux_session_name = DiscordBotMain.getTmuxSessionName();

    // ExecutorService für asynchrone Aufgaben
    private final ExecutorService executorService;
    private final MessageSender messageSender = new MessageSender(logger);


    // Konstruktor, um ExecutorService zu injizieren
    public MessageReact(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) { // @NotNull verwenden
        // Ignoriere Bots und Webhooks
        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        // Nur auf Nachrichten aus Gilden (Servern) reagieren
        if (event.isFromGuild()) {
            String content = event.getMessage().getContentStripped();
            MessageChannelUnion channel = event.getChannel();

            // Prüfe auf Prefix
            if (content.startsWith(prefix)) {
                // Befehl und Argumente trennen
                String[] args = content.substring(prefix.length()).trim().split("\\s+", 2);
                String command = args[0].toLowerCase();
                String commandArgs = (args.length > 1) ? args[1] : null; // Argumente, falls vorhanden

                logger.debug("Guild: '{}', Channel: '#{}', User: '{}' used command: '{}'{}",
                        event.getGuild().getName(),
                        event.getChannel().getName(),
                        event.getAuthor().getAsTag(),
                        command,
                        (commandArgs != null ? " with args: '" + commandArgs + "'" : ""));

                // Befehl ausführen
                switch (command) {
                    case "help", "h":
                        HelpCommand helpCommand = new HelpCommand(messageSender, logger);
                        helpCommand.execute(event, "");
                        break;
                    case "event", "e":
                        if (commandArgs != null && !commandArgs.isBlank()) {
                            PollCommand pollCommand = new PollCommand(messageSender, logger);
                            pollCommand.execute(event, commandArgs);
                        } else {
                            channel.sendMessage("Bitte gib eine Nachricht für die Umfrage an. Beispiel: `" + prefix + "e Sollten wir Pizza bestellen?`").queue();
                        }
                        break;
                    case "info":
                        InfoCommand infoCommand = new InfoCommand(messageSender, logger);
                        infoCommand.execute(event, "");
                        break;
                    case "restart":
                        handleRestartCommand(channel, event);
                        break;
                    case "status":
                        handleStatusCommand(channel, event);
                        break;
                    default:
                        channel.sendMessage(
                                "Unbekannter Befehl: `" + command + "`. Verwende `" + prefix + "help` für eine Liste aller Befehle.")
                                .queue();
                        break;
                }
            }
        }
    }

    // --- Minecraft Server Interaktion (Asynchron) ---

    private void handleStatusCommand(MessageChannelUnion channel, MessageReceivedEvent event) {
        channel.sendTyping().queue(); // Zeigt an, dass der Bot arbeitet
        checkServerStatusAsync().thenAccept(isOnline -> {
            if (isOnline) {
                logger.info("Server Status Check: Online");
                messageSender.sendMessageEmbed(channel, event, Status.ONLINE);
            } else {
                logger.info("Server Status Check: Offline");
                messageSender.sendMessageEmbed(channel, event, Status.OFFLINE);
            }
        }).exceptionally(ex -> {
            logger.error("Fehler bei der asynchronen Statusprüfung: {}", ex.getMessage(), ex);
            messageSender.sendErrorEmbed(channel, event, "Fehler beim Prüfen des Serverstatus.", "Details findest du in den Bot-Logs.");
            return null; // Erforderlich für exceptionally
        });
    }

     private void handleRestartCommand(MessageChannelUnion channel, MessageReceivedEvent event) {
        channel.sendTyping().queue();
        checkServerStatusAsync().thenCompose(isOnline -> {
            if (isOnline) {
                logger.info("Server ist bereits online. Kein Neustart erforderlich.");
                messageSender.sendMessageEmbed(channel, event, Status.ONLINE); // Informiere, dass er schon läuft
                return CompletableFuture.completedFuture(false); // Kein Startversuch nötig
            } else {
                logger.info("Server ist offline. Starte Neustart-Versuch...");
                messageSender.sendMessageEmbed(channel, event, Status.RESTART); // Nachricht, dass der Start versucht wird
                return startMcServerAsync(); // Starte den Server asynchron (gibt true zurück, wenn Befehl gesendet)
            }
        }).thenAccept(startCommandSent -> {
            if (startCommandSent) {
                 logger.info("Startbefehl erfolgreich an tmux gesendet.");
                 // Die RESTART/STARTING Nachricht wurde bereits gesendet.
                 // Optional: Nach kurzer Wartezeit (z.B. 10-15 Sek.) erneut Status prüfen und Feedback geben?
                 // executorService.schedule(() -> handleStatusCommand(channel, event), 15, TimeUnit.SECONDS);
            }
            // Wenn !startCommandSent, wurde die ONLINE Nachricht schon gesendet.
        }).exceptionally(ex -> {
            logger.error("Fehler im Neustart-Prozess: {}", ex.getMessage(), ex);
            messageSender.sendErrorEmbed(channel, event, "Fehler beim Starten des Servers.", "Der Startbefehl konnte nicht gesendet werden oder die Statusprüfung schlug fehl. Details in den Logs.");
            return null;
        });
    }

    // --- Asynchrone Helfermethoden für externe Prozesse ---

    /**
     * Prüft asynchron, ob der Serverprozess läuft (via pgrep).
     * Gibt true zurück, wenn der Prozess gefunden wird, sonst false.
     * Wirft eine Exception bei Fehlern während der Ausführung.
     */
    private CompletableFuture<Boolean> checkServerStatusAsync() {
        return CompletableFuture.supplyAsync(() -> {
            // Wichtig: Sicherstellen, dass 'server_start_script_name' eindeutig genug ist!
            // Ein vollständiger Pfad ist oft besser.
            // Beispiel: pgrep -f "/home/user/mcserver/start2.sh"
            String commandToFind = server_start_script_name;
            String[] cmd = {"/bin/bash", "-c", "pgrep -a -f \"" + commandToFind + "\""};
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
     * Gibt true zurück, wenn der Befehl erfolgreich gesendet wurde (Exit Code 0), sonst false.
     * Wirft eine Exception bei Fehlern während der Ausführung.
     */
    private CompletableFuture<Boolean> startMcServerAsync() {
        return CompletableFuture.supplyAsync(() -> {
            // Stelle sicher, dass der Pfad zum Startskript korrekt ist! Absoluter Pfad empfohlen.
            String commandToSend = "bash " + server_start_script_name; // z.B. bash /home/user/mc/start.sh
            // Der Befehl, der an tmux gesendet wird. 'C-m' simuliert Enter.
            String tmuxCommand = String.format("unset TMUX; tmux send-keys -t %s '%s' C-m",
                                               tmux_session_name, commandToSend);
            String[] cmd = { "/bin/bash", "-c", tmuxCommand };
            logger.info("Sende Startbefehl an tmux: {}", tmuxCommand);
            try {
                // Prüfe zuerst, ob die tmux Session existiert (optional, aber gut)
                String[] checkSessionCmd = {"/bin/bash", "-c", "tmux has-session -t " + tmux_session_name};
                ProcessResult sessionCheck = executeCommand(checkSessionCmd, 3);
                if (sessionCheck.exitCode() != 0) {
                     logger.error("tmux Session '{}' nicht gefunden! Server kann nicht gestartet werden. stderr: {}", tmux_session_name, sessionCheck.stderr());
                     throw new RuntimeException("tmux Session '" + tmux_session_name + "' existiert nicht.");
                }
                logger.debug("tmux Session '{}' gefunden.", tmux_session_name);

                // Session existiert, sende den Startbefehl
                ProcessResult result = executeCommand(cmd, 10); // 10 Sekunden Timeout
                logger.info("tmux send-keys Exit Code: {}", result.exitCode());
                logger.debug("tmux send-keys stdout: {}", result.stdout().isEmpty() ? "<leer>" : result.stdout());
                logger.debug("tmux send-keys stderr: {}", result.stderr().isEmpty() ? "<leer>" : result.stderr());

                if (result.exitCode() == 0) {
                    logger.info("Startbefehl erfolgreich an tmux-Session '{}' gesendet.", tmux_session_name);
                    return true;
                } else {
                    logger.error("Fehler beim Senden des Startbefehls an tmux (Exit Code {}). stderr: {}", result.exitCode(), result.stderr());
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
    private ProcessResult executeCommand(String[] command, long timeoutSeconds) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // Verwende CompletableFuture, um Streams parallel zu lesen
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()), executorService);
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()), executorService);

        boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finishedInTime) {
            process.destroyForcibly(); // Prozess beenden
            // Warte kurz auf die Reader-Futures, aber nicht ewig
            stdoutFuture.completeExceptionally(new RuntimeException("Process timed out"));
            stderrFuture.completeExceptionally(new RuntimeException("Process timed out"));
            throw new RuntimeException("Timeout (" + timeoutSeconds + "s) beim Warten auf Befehl: " + String.join(" ", command));
        }

        // Warte auf Abschluss der Stream-Reader und hole Ergebnisse
        String stdout = stdoutFuture.get(1, TimeUnit.SECONDS); // Kurzer Timeout für get
        String stderr = stderrFuture.get(1, TimeUnit.SECONDS); // Kurzer Timeout für get

        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    /** Liest einen InputStream vollständig und gibt ihn als String zurück. */
    private String readStream(java.io.InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.warn("Fehler beim Lesen eines Prozess-Streams: {}", e.getMessage());
            return "Fehler beim Lesen des Streams: " + e.getMessage();
        }
    }

}