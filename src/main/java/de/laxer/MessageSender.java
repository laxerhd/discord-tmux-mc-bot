package de.laxer;

import java.time.Instant;
import org.slf4j.Logger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MessageSender {
  Logger logger;

  public MessageSender(Logger logger) {
    this.logger = logger;
  }

  public void sendMessageEmbed(MessageChannelUnion channel, MessageReceivedEvent event, Status status, String message) {
    EmbedBuilder eb = new EmbedBuilder();
    eb.setAuthor("Minecraft Server", null, event.getJDA().getSelfUser().getEffectiveAvatarUrl()) // Bot-Avatar
        .setFooter("Angefordert von " + event.getAuthor().getName(), event.getAuthor().getEffectiveAvatarUrl())
        .setTimestamp(Instant.now());

    switch (status) {
      case ONLINE:
        eb.setColor(0x2ecc71) // Grün
            .setTitle("✅ Server ist Online")
            .setDescription("Der Minecraft-Server läuft.\nVerbinde dich mit: `" + Config.server_ip + "`");
        break;
      case RESTART: // Bedeutet jetzt "Wird gestartet"
        eb.setColor(0xf39c12) // Orange
            .setTitle("🚀 Starte Server...")
            .setDescription(
                "Der Befehl zum Starten des Minecraft-Servers wurde gesendet.\nEs kann einen Moment dauern, bis er verfügbar ist.\nServer-IP: `"
                    + Config.server_ip + "`");
        break;
      case OFFLINE:
        eb.setColor(0xe74c3c) // Rot
            .setTitle("❌ Server ist Offline")
            .setDescription("Der Minecraft-Server ist derzeit nicht erreichbar.\nMit `" + Config.prefix
                + "restart` kannst du versuchen, ihn zu starten.\nServer-IP: `" + Config.server_ip + "`");
        break;
      case INFO:
        eb.setTitle("🤖 Bot Information")
            .setColor(0x3498db) // Blau
            .setDescription("Ich bin ein Bot, der den Minecraft Server verwalten kann.")
            .addField("Entwickler", "Laxer", true)
            .addField("Version", "1.0.0", true) // Oder aus pom.xml holen?
            .addField("Prefix", "`" + Config.prefix + "`", true)
            .addField("Server IP", "`" + Config.server_ip + "`", false)
            .addField("Ping", event.getJDA().getGatewayPing() + "ms", true);
        break;
      case HELP:
        StringBuilder description = new StringBuilder("Hier sind alle Befehle, die du verwenden kannst:\n\n");
        Config.commands.forEach((cmd, desc) -> description.append("`").append(Config.prefix).append(cmd).append("` - ")
            .append(desc).append("\n"));
        eb.setTitle("🤖 Bot Hilfe")
            .setColor(0x0099FF) // Helles Blau
            .setDescription(description.toString())
            .setFooter("Angefordert von " + event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
            .setTimestamp(Instant.now()); // Aktueller Zeitstempel
        break;
      case POLL:
        deleteMessage(event);
        eb.setAuthor(event.getAuthor().getName() + " fragt:", null, event.getAuthor().getEffectiveAvatarUrl())
            .setColor(0x0099FF)
            .setDescription("**" + message + "**") // Nachricht hervorheben
            .setFooter("Abstimmung gestartet")
            .setTimestamp(Instant.now());
        break;
    }
    channel.sendMessageEmbeds(eb.build()).queue(sentMessage -> {
      if (status == Status.POLL) {
        reactMessagePoll(event, channel, eb, sentMessage, message);
      }
    },
        failure -> logger.error("Konnte Embed-Nachricht nicht senden (Status {}): {}", status, failure.getMessage()));
  }

  /** Sendet eine Fehler-Embed-Nachricht */
  public void sendErrorEmbed(MessageChannelUnion channel, MessageReceivedEvent event, String title,
      String description) {
    EmbedBuilder eb = new EmbedBuilder();
    eb.setColor(0xe74c3c) // Rot
        .setTitle("⚠️ Fehler: " + title)
        .setDescription(description)
        .setFooter("Bei Problemen, kontaktiere den Bot-Admin.")
        .setTimestamp(Instant.now());
    channel.sendMessageEmbeds(eb.build()).queue();
  }

  public void reactMessagePoll(MessageReceivedEvent event, MessageChannelUnion channel, EmbedBuilder eb,
      Message sentMessage, String message) {
    // Embed senden und Reaktionen hinzufügen
    sentMessage.addReaction(Emoji.fromUnicode("✅")).queue(); // Grüner Haken
    sentMessage.addReaction(Emoji.fromUnicode("❌")).queue(); // Rotes Kreuz
    logger.info("Umfrage '{}' erfolgreich erstellt von {}", message, event.getAuthor().getAsTag());
  }

  public void deleteMessage(MessageReceivedEvent event) {
    event.getMessage().delete().queue(
        success -> logger.debug("Originalnachricht für Umfrage gelöscht."),
        failure -> logger.warn("Konnte Originalnachricht für Umfrage nicht löschen: {}", failure.getMessage()));
  }
}
