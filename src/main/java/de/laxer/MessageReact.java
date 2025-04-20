package de.laxer;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutorService;

public class MessageReact extends ListenerAdapter {

    // SLF4j Logger
    private static final Logger logger = LoggerFactory.getLogger(MessageReact.class);

    // Konfiguration aus DiscordBotMain holen
    private final String prefix = Config.prefix;

    // ExecutorService für asynchrone Aufgaben
    private final MessageSender messageSender = new MessageSender(logger);
    private final MinecraftSessionHandler sessionHandler;

    // Commands
    Command helpCommand;
    Command pollCommand;
    Command restartCommand;
    Command infoCommand;
    Command statusCommand;

    // Konstruktor, um ExecutorService zu injizieren
    public MessageReact(ExecutorService executorService) {
        sessionHandler = new MinecraftSessionHandler(logger, executorService);

        helpCommand = new HelpCommand(messageSender, logger);
        pollCommand = new PollCommand(messageSender, logger);
        restartCommand = new RestartCommand(messageSender, logger, executorService, sessionHandler);
        infoCommand = new InfoCommand(messageSender, logger);
        statusCommand = new StatusCommand(messageSender, logger, sessionHandler);
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
                        helpCommand.execute(event, "");
                        break;
                    case "event", "e":
                        if (commandArgs != null && !commandArgs.isBlank()) {
                            pollCommand.execute(event, commandArgs);
                        } else {
                            channel.sendMessage("Bitte gib eine Nachricht für die Umfrage an. Beispiel: `" + prefix + "e Sollten wir Pizza bestellen?`").queue();
                        }
                        break;
                    case "info":
                        infoCommand.execute(event, "");
                        break;
                    case "restart":
                        restartCommand.execute(event, "");
                        break;
                    case "status":
                        statusCommand.execute(event, "");
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
}