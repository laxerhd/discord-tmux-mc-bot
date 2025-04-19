package de.laxer;

import java.time.Instant;

import org.slf4j.Logger; // SLF4j Logger
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MessageSender {
        /** Sendet eine Standard-Status-Embed-Nachricht */
    public void sendMessageEmbed(MessageChannelUnion channel, MessageReceivedEvent event, Status status, Logger logger) {
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
                   .setDescription("Der Befehl zum Starten des Minecraft-Servers wurde gesendet.\nEs kann einen Moment dauern, bis er verfügbar ist.\nServer-IP: `" + Config.server_ip + "`");
                 break;
            case OFFLINE:
                eb.setColor(0xe74c3c) // Rot
                  .setTitle("❌ Server ist Offline")
                  .setDescription("Der Minecraft-Server ist derzeit nicht erreichbar.\nMit `" + Config.prefix + "restart` kannst du versuchen, ihn zu starten.\nServer-IP: `" + Config.server_ip + "`");
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
        }
        channel.sendMessageEmbeds(eb.build()).queue(
            success -> {}, // Nichts tun bei Erfolg
            failure -> logger.error("Konnte Embed-Nachricht nicht senden (Status {}): {}", status, failure.getMessage())
        );
    }
}
