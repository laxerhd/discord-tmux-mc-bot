package de.laxer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

public class MessageReact extends ListenerAdapter {

    private static String date_format = "dd.MM.yyyy | kk:mm";
    private static String time_format = "kk:mm";

    private static final Logger logger = Logger.getLogger(MessageReact.class.getName());
    private final String prefix = DiscordBotMain.getPrefix();
    private final String server_ip = DiscordBotMain.getServer_ip();
    private final String server_start_script_name = DiscordBotMain.getServerStartingScriptName();

    private final Map<String, String> commands = new HashMap<>() {
        {
            put("help", "Zeigt alle verfügbaren Befehle an.");
            put("e", "Erstellt eine Umfrage.");
            put("info", "Zeigt Informationen über den Bot an.");
            put("restart", "Restartet den Minecraft-Server");
            put("status", "Checkt den Status des Minecraft-Servers ab");
        }
    };

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            String content = event.getMessage().getContentStripped();
            MessageChannelUnion channel = event.getChannel();

            if (content.startsWith(prefix)) {
                String[] args = content.split(" ", 2);
                String command = args[0].substring(prefix.length()).toLowerCase();
                logger.info("Used command was: " + command);

                switch (command) {
                    case "help":
                        showHelp(channel, event);
                        break;
                    case "e":
                        if (args.length > 1) {
                            createPoll(channel, event, args[1]);
                        } else {
                            channel.sendMessage("Bitte geben Sie eine Nachricht für die Umfrage an.").queue();
                        }
                        break;
                    case "info":
                        showBotInfo(channel, event);
                        break;
                    case "restart":
                        restartMcServer(channel, event);
                        break;
                    case "status":
                        // checkMcStatus(channel, event);
                        checkStatus(channel, event);
                        break;
                    default:
                        channel.sendMessage(
                                "Unbekannter Befehl. Verwenden Sie " + prefix + "h für eine Liste der Befehle.")
                                .queue();
                        break;
                }
            }
        }
    }

    private void showHelp(MessageChannelUnion channel, MessageReceivedEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder description = new StringBuilder("Präfix: " + prefix + "\n**Befehle:**\n");

        for (Map.Entry<String, String> command : commands.entrySet()) {
            description.append(prefix).append(command.getKey()).append(" - ").append(command.getValue()).append("\n");
        }

        eb.setAuthor("Alle Befehle")
                .setColor(0x0099FF)
                .setFooter(event.getMessage().getTimeCreated().format(DateTimeFormatter.ofPattern(date_format)))
                .setDescription(description.toString());

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void createPoll(MessageChannelUnion channel, MessageReceivedEvent event, String message) {
        EmbedBuilder eb = new EmbedBuilder();

        // Lösche die Originalnachricht
        event.getMessage().delete().queue(
                success -> logger.info("Originalnachricht gelöscht."),
                failure -> logger.warning("Fehler beim Löschen der Originalnachricht: " + failure.getMessage()));

        // Erstelle eine neue Embed-Nachricht
        eb.setAuthor(event.getAuthor().getName())
                .setColor(0x0099FF)
                .setFooter(event.getMessage().getTimeCreated().format(DateTimeFormatter.ofPattern(date_format)))
                .setThumbnail(Objects.requireNonNull(event.getAuthor().getAvatarUrl()))
                .setDescription(message);

        // Sende die neue Nachricht und füge Reaktionen hinzu
        channel.sendMessageEmbeds(eb.build()).queue(m -> {
            m.addReaction(Emoji.fromFormatted("✅")).queue();
            m.addReaction(Emoji.fromFormatted("❌")).queue();
        });
        logger.info("Poll erfolgreich eröffnet!");
    }

    private void showBotInfo(MessageChannelUnion channel, MessageReceivedEvent event) {
        sendMessage(channel, event, Status.INFO);
    }

    private void checkStatus(MessageChannelUnion channel, MessageReceivedEvent event) {
        if (checkServerStatus()) {
            logger.info("Server ist noch Online!");
            sendMessage(channel, event, Status.ONLINE);
        } else {
            logger.info("Server ist noch Offline!");
            sendMessage(channel, event, Status.OFFLINE);
        }
    }

    private boolean checkServerStatus() {
        try {
            Process process = Runtime.getRuntime().exec("pgrep -a -f " + server_start_script_name);
            int exitCode = process.waitFor();
            logger.info("pgrep exit code was: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            logger.warning("Fehler beim Überprüfen des Server-Status: " + e.getMessage());
            return false;
        }
    }

    private boolean restartMcServer(MessageChannelUnion channel, MessageReceivedEvent event) {
        logger.info("Überprüfe Status von Minecraft Server");
        if (checkServerStatus()) {
            logger.info("Server ist noch Online!");
            // Server online msg
            sendMessage(channel, event, Status.ONLINE);
        } else {
            try {
                logger.info("Server ist Offline!");
                sendMessage(channel, event, Status.RESTART);
                // Entferne die TMUX-Umgebungsvariable und sende den Startbefehl an die
                // tmux-Session 'mcserver'
                logger.info("Neustart-Command an Tmux Session senden...");
                String[] cmd = { "/bin/bash", "-c",
                        "unset TMUX; tmux send-keys -t mcserver 'bash " + server_start_script_name + "' C-m" };
                Process process = Runtime.getRuntime().exec(cmd);
                int exitCode = process.waitFor();
                logger.info("Gesendet!");
                if (exitCode == 0) {
                    logger.info("Minecraft-Server wurde erfolgreich in der tmux-Session 'mcserver' gestartet.");
                } else {
                    logger.warning("Fehler beim Starten des Minecraft-Servers: Exit Code " + exitCode);
                }
            } catch (Exception e) {
                logger.warning("Crashed: Fehler beim Starten des Minecraft-Servers: " + e.getMessage());
            }
        }
        return true;
    }

    private void sendMessage(MessageChannelUnion channel, MessageReceivedEvent event, Status status) {
        switch (status) {
            case ONLINE:
                EmbedBuilder eb_online = new EmbedBuilder();
                eb_online.setAuthor("Minecraft-Server")
                        .setColor(0x2ab868)
                        .setDescription("Der MC-Server läuft bereits.\nMit folgender IP kannst du dich verbinden: "
                                + server_ip);
                channel.sendMessageEmbeds(eb_online.build()).queue();
                break;
            case RESTART:
                EmbedBuilder eb_restart = new EmbedBuilder();
                eb_restart.setAuthor("Minecraft-Server")
                        .setColor(0xbf3134)
                        .setDescription(
                                "Der Minecraft Server wird neugestartet.\nMit folgender IP kannst du ich verbinden: "
                                        + server_ip);
                channel.sendMessageEmbeds(eb_restart.build()).queue();
                break;
            case OFFLINE:
                EmbedBuilder eb_offline = new EmbedBuilder();
                eb_offline.setAuthor("Minecraft-Server")
                        .setColor(0xbf3134)
                        .setDescription(
                                "Der Minecraft Server ist zur Zeit offline.\nMit $restart kannst du ihn starten.\nServer-IP: "
                                        + server_ip);
                channel.sendMessageEmbeds(eb_offline.build()).queue();
                break;
            case INFO:
                EmbedBuilder eb_info = new EmbedBuilder();
                eb_info.setAuthor("----------------------- Bot Information -----------------------")
                        .setColor(0x00FF00)
                        .setFooter(event.getMessage().getTimeCreated().format(DateTimeFormatter.ofPattern(date_format)))
                        .setDescription("Mein cooler Discord-Bot")
                        .addField("Ersteller", "Laxer", false)
                        .addField("Version", "Alpha", false);

                channel.sendMessageEmbeds(eb_info.build()).queue();
                break;
        }
    }

}
