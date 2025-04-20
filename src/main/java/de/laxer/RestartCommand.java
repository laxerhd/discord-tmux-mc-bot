package de.laxer;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RestartCommand extends Command {
    private final MinecraftSessionHandler sessionHandler;

    public RestartCommand(MessageSender messageSender, Logger logger, MinecraftSessionHandler sessionHandler) {
        super(messageSender, logger);
        this.sessionHandler = sessionHandler;
    }

    @Override 
    public void execute(MessageReceivedEvent event, String message) {
        handleRestartCommand(event.getChannel(), event);
    }

    private void handleRestartCommand(MessageChannelUnion channel, MessageReceivedEvent event) {
        channel.sendTyping().queue();
        sessionHandler.checkServerStatusAsync().thenCompose(isOnline -> {
            if (isOnline) {
                logger.info("Server ist bereits online. Kein Neustart erforderlich.");
                messageSender.sendMessageEmbed(channel, event, Status.ONLINE, ""); // Informiere, dass er schon läuft
                return CompletableFuture.completedFuture(false); // Kein Startversuch nötig
            } else {
                logger.info("Server ist offline. Starte Neustart-Versuch...");
                messageSender.sendMessageEmbed(channel, event, Status.RESTART, ""); // Nachricht, dass der Start versucht wird
                return sessionHandler.startMcServerAsync(); // Starte den Server asynchron (gibt true zurück, wenn Befehl gesendet)
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
}
