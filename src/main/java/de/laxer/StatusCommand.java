package de.laxer;

import org.slf4j.Logger;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class StatusCommand extends Command {

    private final MinecraftSessionHandler sessionHandler;

    public StatusCommand(MessageSender messageSender, Logger logger, MinecraftSessionHandler sessionHandler) {
        super(messageSender, logger);
        this.sessionHandler = sessionHandler;
    }

    @Override
    public void execute(MessageReceivedEvent event, String message) {
        handleStatusCommand(event.getChannel(), event);
    }

    private void handleStatusCommand(MessageChannelUnion channel, MessageReceivedEvent event) {
        channel.sendTyping().queue(); // Zeigt an, dass der Bot arbeitet
        sessionHandler.checkServerStatusAsync().thenAccept(isOnline -> {
            if (isOnline) {
                logger.info("Server Status Check: Online");
                messageSender.sendMessageEmbed(channel, event, Status.ONLINE, "");
            } else {
                logger.info("Server Status Check: Offline");
                messageSender.sendMessageEmbed(channel, event, Status.OFFLINE, "");
            }
        }).exceptionally(ex -> {
            logger.error("Fehler bei der asynchronen Statusprüfung: {}", ex.getMessage(), ex);
            messageSender.sendErrorEmbed(channel, event, "Fehler beim Prüfen des Serverstatus.", "Details findest du in den Bot-Logs.");
            return null;
        });
    }
    
}
